/*
 * File Monitor - Watches a folder and notify files changes
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
package it.fridrik.filemonitor;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * FileMonitor (the name says it all) monitors a folder and its subfolders for
 * file changes (added, removed and modified). Only one file extension can be
 * monitored for each instance of FileMonitor. For each change found, an event
 * is raised. File renames are notified as a file removal and a file addition,
 * in this order. FileMonitor implements Runnable and expects you to start it
 * through a ScheduledExecutorService
 * 
 * @author Federico Fissore (federico@fissore.org)
 * @since 1.0
 */
public class FileMonitor implements Runnable {

  private class FolderWatcher {
    private final File folder;
    private final HashMap<File, Long> fileMap = new HashMap<File, Long>();
    
    private FolderWatcher(File folder) {
      this.folder = folder;
    }

    /**
     * Checks for files deletion
     */
    protected void checkDeletion() {
      List<File> filesToDelete = new LinkedList<File>();
      for (File file : fileMap.keySet()) {
        if (!file.exists()) {
          filesToDelete.add(file);
          notifyDeletedListeners(new FileEvent(folder, file));
        }
      }
      
      for (File file : filesToDelete) {
        fileMap.remove(file);
      }
    }

    protected void checkAddAndModify() {
      checkAddAndModify(folder);
    }
    
    /**
     * Checks for file addition and modification
     * 
     * @param currentFolder
     *          the folder to monitor
     */
    protected void checkAddAndModify(File currentFolder) {
      for (File file : getFiles(currentFolder)) {
        if (file.isDirectory()) {
          checkAddAndModify(file);
        } else {
          Long lastModified = fileMap.get(file);
          if (lastModified != null) {
            if (lastModified != file.lastModified()) {
              fileMap.put(file, file.lastModified());
              notifyModifiedListeners(new FileEvent(folder, file));
            }
          } else {
            fileMap.put(file, file.lastModified());
            notifyAddedListeners(new FileEvent(folder, file));
          }
        }
      }
    }

    public File[] getFiles(File folder) {
      return folder.listFiles(filenameFilter);
    }
  }
  
	private final ArrayList<FolderWatcher> folders = new ArrayList<FolderWatcher>();
	private final ExtFilenameFilter filenameFilter;
	private final String fileExtension;
	private final List<FileAddedListener> fileAddedListeners;
	private final List<FileDeletedListener> fileDeletedListeners;
	private final List<FileModifiedListener> fileModifiedListeners;

	class ExtFilenameFilter implements FilenameFilter {

		@SuppressWarnings("synthetic-access")
		public boolean accept(File folder, String name) {
			return name.endsWith(fileExtension)
					|| new File(folder.getAbsolutePath() + File.separator + name)
							.isDirectory();
		}

	}

	/**
	 * Creates a new instance of FileMonitor
	 * 
	 * @param folderPaths
	 *          the folder paths to monitor
	 * @param fileExtension
	 *          the file extension to monitor
	 */
	public FileMonitor(List<String> folderPaths, String fileExtension) {
		this.fileExtension = fileExtension;
		this.filenameFilter = new ExtFilenameFilter();
		this.fileAddedListeners = new LinkedList<FileAddedListener>();
		this.fileDeletedListeners = new LinkedList<FileDeletedListener>();
		this.fileModifiedListeners = new LinkedList<FileModifiedListener>();
		
		for (String path : folderPaths) {
		  File folder = new File(path);
  		if (!folder.isAbsolute() || !folder.isDirectory()) {
  			throw new IllegalArgumentException("The parameter with value "
  					+ path + " MUST be a folder");
  		}
  		
  		folders.add(new FolderWatcher(folder));
		}
	}

	public void run() {
		for (FolderWatcher folder : folders) {
	    folder.checkDeletion();
		  folder.checkAddAndModify();
		}
	}

	/**
	 * Adds a file modified listener
	 * 
	 * @param listener
	 *          the listener
	 */
	public void addModifiedListener(FileModifiedListener listener) {
		fileModifiedListeners.add(listener);
	}

	/**
	 * Adds a file deleted listener
	 * 
	 * @param listener
	 *          the listener
	 */
	public void addDeletedListener(FileDeletedListener listener) {
		fileDeletedListeners.add(listener);
	}

	/**
	 * Adds a file added listener
	 * 
	 * @param listener
	 *          the listener
	 */
	public void addAddedListener(FileAddedListener listener) {
		fileAddedListeners.add(listener);
	}

	private void notifyModifiedListeners(FileEvent event) {
		for (FileModifiedListener listener : fileModifiedListeners) {
			listener.fileModified(event);
		}
	}

	private void notifyAddedListeners(FileEvent event) {
		for (FileAddedListener listener : fileAddedListeners) {
			listener.fileAdded(event);
		}
	}

	private void notifyDeletedListeners(FileEvent event) {
		for (FileDeletedListener listener : fileDeletedListeners) {
			listener.fileDeleted(event);
		}
	}
}
