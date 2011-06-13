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
import java.util.EventObject;

/**
 * Raised every time a file is added, deleted or modified
 * 
 * @author Federico Fissore (federico@fissore.org)
 * @since 1.0
 */
public class FileEvent extends EventObject {
    private static final long serialVersionUID = 4696923746078504205L;

    private final File baseFolder;
    
    /**
     * Creates a new FileEvent.
     * 
     * @param baseFolder
     *          the base folder where the changed file was found
     * @param fullPath
     *          the absolute path of the changed file
     */
    public FileEvent(File baseFolder, File fullPath) {
        super(fullPath);
        this.baseFolder = baseFolder;
    }

    /**
     * Gets the changed file.
     */
    @Override
    public File getSource() {
      return (File) super.getSource();
    }
    
    public File getBaseFolder() {
      return baseFolder;
    }
}
