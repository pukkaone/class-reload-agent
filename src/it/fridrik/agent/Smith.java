/*
 * Agent Smith - A java hot class redefinition implementation
 * Copyright (C) 2007 Federico Fissore
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software 
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package it.fridrik.agent;

import it.fridrik.filemonitor.FileEvent;
import it.fridrik.filemonitor.FileModifiedListener;
import it.fridrik.filemonitor.FileMonitor;
import it.fridrik.filemonitor.JarEvent;
import it.fridrik.filemonitor.JarModifiedListener;
import it.fridrik.filemonitor.JarMonitor;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Agent Smith is an agent with just one aim: redefining classes as soon as they
 * are changed. Smith bundles together Instrumentation, FileMonitor and
 * JarMonitor
 * 
 * @author Federico Fissore (federico@fissore.org)
 * @see FileMonitor
 * @see JarMonitor
 * @since 1.0
 */
public class Smith implements FileModifiedListener, JarModifiedListener {

	/** Min period allowed */
	private static final int MONITOR_PERIOD_MIN_VALUE = 1000;

	/** Lists of active Smith agents */
	private static Vector<Smith> smiths = new Vector<Smith>();

	/** Called when the agent is initialized via command line */
	public static void premain(String agentArgs, Instrumentation inst) {
		initialize(agentArgs, inst);
	}

	/** Called when the agent is initialized after the jvm startup */
	public static void agentmain(String agentArgs, Instrumentation inst) {
		initialize(agentArgs, inst);
	}

	private static void initialize(String agentArgs, Instrumentation inst) {
		SmithArgs args = new SmithArgs(agentArgs);

		if (!args.isValid()) {
			throw new RuntimeException(
					"Your parameters are invalid! Check the documentation for the correct syntax");
		}

		Smith smith = new Smith(inst, args);
		smiths.add(smith);
	}

	/** Stops all active Smith agents */
	public static void stopAll() {
		for (Smith smith : smiths) {
			smith.stop();
		}
	}

	private static final Logger log = Logger.getLogger(Smith.class.getName());
	private final Instrumentation inst;
	private final List<String> classFolders;
	private final String jarFolder;
	private final ScheduledExecutorService service;
	private HashMap<String, Class<?>> loadedClassesMap;

	/**
	 * Creates and starts a new Smith agent. Please note that periods smaller than
	 * 500 (milliseconds) won't be considered.
	 * 
	 * @param inst
	 *          the instrumentation implementation
	 * @param args
	 *          the {@link SmithArgs} instance
	 */
	public Smith(Instrumentation inst, SmithArgs args) {
		this.inst = inst;
		this.classFolders = args.getClassFolders();
		this.jarFolder = args.getJarFolder();
		int monitorPeriod = MONITOR_PERIOD_MIN_VALUE;
		if (args.getPeriod() > monitorPeriod) {
			monitorPeriod = args.getPeriod();
		}

		log.setLevel(args.getLogLevel());

		service = Executors.newScheduledThreadPool(2);

		FileMonitor fileMonitor = new FileMonitor(classFolders, "class");
		fileMonitor.addModifiedListener(this);
		service.scheduleWithFixedDelay(fileMonitor, 0, monitorPeriod,
				TimeUnit.MILLISECONDS);

		if (jarFolder != null) {
			JarMonitor jarMonitor = new JarMonitor(jarFolder);
			jarMonitor.addJarModifiedListener(this);
			service.scheduleWithFixedDelay(jarMonitor, 0, monitorPeriod,
					TimeUnit.MILLISECONDS);
		}

		log.info("Smith: watching class folders: " + classFolders);
		log.info("Smith: watching jars folder: " + jarFolder);
		log.info("Smith: period between checks (ms): " + monitorPeriod);
		log.info("Smith: log level: " + log.getLevel());
	}

	/**
	 * Stops this Smith agent
	 */
	public void stop() {
		service.shutdown();
	}

	/**
	 * When the monitor notifies of a changed class file, Smith will redefine it
	 */
	public void fileModified(FileEvent event) {
	  File classFile = event.getSource();
	  String className = toClassName(event.getBaseFolder().toString(), classFile.toString());
    try {
      byte[] classBytes = toByteArray(new FileInputStream(classFile));
      redefineClass(className, classBytes);
    } catch (Exception e) {
      log.log(Level.SEVERE, "fileModified", e);
    }
	}

	/**
	 * When the monitor notifies of a changed jar file, Smith will redefine the
	 * changed class file the jar contains
	 */
	public void jarModified(JarEvent event) {
    String className = toClassName(null, event.getEntryName());
	  JarFile jar = event.getSource();
    try {
      byte[] classBytes = toByteArray(
          jar.getInputStream(
              getJarEntry(jar, event.getEntryName())));
  		redefineClass(className, classBytes);
    } catch (Exception e) {
      log.log(Level.SEVERE, "jarModified", e);
    }
	}

	/**
	 * Redefines the specified class
	 * 
	 * @param className
	 *          the class name to redefine
	 * @param classBytes
	 *          the compiled class to redefine
	 * @throws ClassNotFoundException
	 *           if the class name cannot be found
	 * @throws UnmodifiableClassException
	 *           if the class is unmodifiable
	 */
	protected void redefineClass(String className, byte[] classBytes)
	  throws ClassNotFoundException, UnmodifiableClassException
	{
	  HashMap<String, Class<?>> loadedClassesMap = getLoadedClassesMap();
	  Class<?> clazz = loadedClassesMap.get(className);
	  if (clazz != null) {
      ClassDefinition definition = new ClassDefinition(clazz, classBytes);
      inst.redefineClasses(new ClassDefinition[] { definition });

      log.info("Redefined " + className);
	  }
	}

	private HashMap<String, Class<?>> getLoadedClassesMap() {
	  if (loadedClassesMap == null) {
	    loadedClassesMap = new HashMap<String, Class<?>>();
	    Class<?>[] loadedClasses = inst.getAllLoadedClasses();
	    for (Class<?> clazz : loadedClasses) {
	      loadedClassesMap.put(clazz.getName(), clazz);
	    }
	  }
	  return loadedClassesMap;
	}

	/**
	 * Converts an absolute path to a file to a fully qualified class name
	 * 
   * @param baseFolder
   *          the base folder where the file was found
	 * @param fullPath
	 *          the absolute path of the file
	 * @return a fully qualified class name
	 */
	private static String toClassName(String baseFolder, String fullPath) {
	  String className = fullPath;
	  if (baseFolder != null) {
	    className = className.substring(baseFolder.length() + 1);
	  }
		return className.replace(".class", "").replace(File.separatorChar, '.');
	}

	/**
	 * Gets the specified jar entry from the specified jar file
	 * 
	 * @param jar
	 *          the jar file that contains the jar entry
	 * @param entryName
	 *          the name of the entry contained in the jar file
	 * @return a JarEntry
	 * @throws IllegalArgumentException
	 *           if the specified entryname is not contained in the specified jar
	 *           file
	 */
	private static JarEntry getJarEntry(JarFile jar, String entryName) {
		JarEntry entry = null;
		for (Enumeration<JarEntry> entries = jar.entries(); entries
				.hasMoreElements();) {
			entry = entries.nextElement();
			if (entry.getName().equals(entryName)) {
				return entry;
			}
		}
		throw new IllegalArgumentException("EntryName " + entryName
				+ " does not exist in jar " + jar);
	}

	/**
	 * Loads .class files as byte[]
	 * 
	 * @param is
	 *          the inputstream of the bytes to load
	 * @return a byte[]
	 * @throws IOException
	 *           if an error occurs while reading file
	 */
	private static byte[] toByteArray(InputStream is) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

		byte[] buffer = new byte[1024];
		int bytesRead = 0;
		while ((bytesRead = is.read(buffer)) != -1) {
			byte[] tmp = new byte[bytesRead];
			System.arraycopy(buffer, 0, tmp, 0, bytesRead);
			baos.write(tmp);
		}

		byte[] result = baos.toByteArray();

		baos.close();
		is.close();

		return result;
	}
}
