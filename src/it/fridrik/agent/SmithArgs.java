/*
 * SmithArgs: the arguments parser of Smith
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * SmithArgs takes care about the parameters you use to start Smith, parsing and
 * making them available with some getters
 * 
 * @author Federico Fissore (federico@fissore.org)
 * @since 1.0
 */
public class SmithArgs {

    private static final String KEY_CLASSES = "classes";
    private static final String KEY_JARS = "jars";
    private static final String KEY_PERIOD = "period";
    private static final String KEY_LOG_LEVEL = "loglevel";
    
    private List<String> classFolders;
    private String jarFolder;
    private int period;
    private Level logLevel;

    private SmithArgs() {
        this.classFolders = new ArrayList<String>();
        this.jarFolder = null;
        this.period = -1;
        this.logLevel = Level.WARNING;
    }

    public SmithArgs(String agentArgs) {
        this();

        if (agentArgs == null || agentArgs.length() == 0) {
            return;
        }
        
        String[] args = agentArgs.split(",");
        for (String arg : args) {
            String[] parts = arg.split("=");
            String name = parts[0].trim();
            String value = parts[1];

            if (name.equals(KEY_CLASSES)) {
                setClassFolders(value);
            }
        
            if (name.equals(KEY_JARS)) {
                setJarFolder(value);
            }
        
            if (name.equals(KEY_PERIOD)) {
                setPeriod(value);
            }
        
            if (name.equals(KEY_LOG_LEVEL)) {
                setLogLevel(value);
            }
        }
    }

    public SmithArgs(
        String classFolder, String jarFolder, int period,   String logLevel)
    {
        this();

        addClassFolder(classFolder);
        setJarFolder(jarFolder);
        setLogLevel(logLevel);
        this.period = period;
    }

    public List<String> getClassFolders() {
        return classFolders;
    }

    public String getJarFolder() {
        return jarFolder;
    }

    public Level getLogLevel() {
        return logLevel;
    }

    public int getPeriod() {
        return period;
    }

    public boolean isValid() {
        return !classFolders.isEmpty();
    }

    private void setClassFolders(String paths) {
        String[] folders = paths.split(File.pathSeparator);
        for (String folder : folders) {
            addClassFolder(folder);
        }
    }

    private void addClassFolder(String classFolder) {
        this.classFolders.add(parseFolderPath(classFolder));
    }

    private void setJarFolder(String jarFolder) {
        this.jarFolder = parseFolderPath(jarFolder);
    }

    private void setLogLevel(String logLevel) {
        try {
            this.logLevel = Level.parse(logLevel.trim());
        } catch (Exception e) {
            this.logLevel = Level.WARNING;
        }
    }

    private void setPeriod(String period) {
        try {
            this.period = Integer.parseInt(period.trim());
        } catch (NumberFormatException e) {
            this.period = -1;
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append(KEY_CLASSES).append("=").append(classFolders);

        if (jarFolder != null) {
            sb.append(",").append(KEY_JARS).append("=").append(jarFolder);
        }

        sb.append(",").append(KEY_PERIOD).append("=").append(period);
        sb.append(",").append(KEY_LOG_LEVEL).append("=")
                .append(logLevel.toString());

        return sb.toString();
    }

    private static String parseFolderPath(String folder) {
        if (folder != null) {
            String trimmed = folder.trim();
            return trimmed.endsWith(File.separator)
                ? trimmed.substring(0, trimmed.length() - 1)
                : trimmed;
        }
        return null;
    }
}
