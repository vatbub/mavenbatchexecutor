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


import org.apache.maven.model.Model;
import org.jetbrains.annotations.NotNull;

public class MavenModelWrapper {
    private Project parentProject;
    @NotNull
    private Model model;

    public MavenModelWrapper(@NotNull Model model) {
        this.model = model;
    }

    public Project getParentProject() {
        return parentProject;
    }

    public void setParentProject(Project parentProject) {
        this.parentProject = parentProject;
    }

    @NotNull
    public Model getModel() {
        return model;
    }

    @Override
    public String toString() {
        return getModel().toString();
    }

    public void setModel(@NotNull Model model) {
        this.model = model;
    }
}
