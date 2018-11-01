package com.github.vatbub.mavenbatchexecutor.core;

/*-
 * #%L
 * maven-batch-executor.core
 * %%
 * Copyright (C) 2016 - 2018 Frederik Kammel
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Project {
    private File projectFolder;
    private List<Project> dependencies;

    public Project(File projectFolder){
        setProjectFolder(projectFolder);
    }

    public Project(String projectFolder){
        setProjectFolder(projectFolder);
    }

    public File getProjectFolder() {
        return projectFolder;
    }

    public void setProjectFolder(File projectFolder) {
        this.projectFolder = projectFolder;
    }

    public void setProjectFolder(String projectFolder) {
        setProjectFolder(new File(projectFolder));
    }

    public List<Project> getDependencies() {
        if (dependencies==null)
            dependencies = new ArrayList<>();
        return dependencies;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Project project = (Project) o;
        return Objects.equals(getProjectFolder(), project.getProjectFolder());
    }

    @Override
    public int hashCode() {

        return Objects.hash(getProjectFolder());
    }

    @Override
    public String toString() {
        return getProjectFolder().getAbsolutePath();
    }
}
