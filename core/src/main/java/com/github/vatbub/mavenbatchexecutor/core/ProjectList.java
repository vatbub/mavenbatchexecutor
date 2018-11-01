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


import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ProjectList extends ArrayList<Project> {
    public ProjectList(int initialCapacity) {
        super(initialCapacity);
    }

    public ProjectList() {
    }

    public ProjectList(@NotNull Collection<? extends Project> c) {
        super(c);
    }

    private void readPOMs(Project parent, File baseDir, List<MavenModelWrapper> pomList, List<MavenModelWrapper> submodulePomList, boolean readAsSubmodules) throws IOException, XmlPullParserException {
        List<MavenModelWrapper> pomListToUse = readAsSubmodules ? submodulePomList : pomList;
        File pomFile = baseDir.toPath().resolve("pom.xml").toFile();
        if (!pomFile.exists()) {
            System.out.println("(" + baseDir + "): POM not found, skipping!");
            pomListToUse.add(null);
            return;
        }

        FileReader reader = new FileReader(pomFile);
        Model model = new MavenXpp3Reader().read(reader);
        model.setPomFile(pomFile);
        MavenModelWrapper wrapper = new MavenModelWrapper(model);
        if (readAsSubmodules)
            wrapper.setParentProject(parent);
        pomListToUse.add(wrapper);
        reader.close();

        for (String module : model.getModules()) {
            readPOMs(parent, baseDir.toPath().resolve(module).toFile(), pomListToUse, submodulePomList, true);
        }
    }

    public ProjectList getSmartOrder() throws IOException, XmlPullParserException {
        List<MavenModelWrapper> pomList = new ArrayList<>(this.size());
        List<MavenModelWrapper> submodulePomList = new ArrayList<>(this.size());

        for (Project project : this) {
            readPOMs(project, project.getProjectFolder(), pomList, submodulePomList, false);
        }

        for (int i = 0; i < this.size(); i++) {
            MavenModelWrapper model = pomList.get(i);

            if (model == null)
                continue;

            Project project = this.get(i);
            int minIndex = -1;

            // check for parent
            Parent parent = model.getModel().getParent();
            if (parent != null) {
                int parentIndex = findIndexOfParent(pomList, parent);
                minIndex = Math.max(parentIndex, minIndex);
                if (parentIndex > -1)
                    project.getDependencies().add(this.get(parentIndex));
            }

            // check dependencies and modules
            minIndex = Math.max(getMinIndexOfModules(project, model.getModel(), pomList, submodulePomList, minIndex), minIndex);

            if (minIndex > i) {
                ProjectList thisCopy = new ProjectList(this);
                Collections.swap(thisCopy, i, minIndex);
                return thisCopy.getSmartOrder();
            }
        }

        return this;
    }

    private int getMinIndexOfModules(Project project, File basedir, List<MavenModelWrapper> smartListPoms, List<MavenModelWrapper> submodulePoms, int minIndex) throws IOException, XmlPullParserException {
        File pomFile = basedir.toPath().resolve("pom.xml").toFile();
        FileReader reader = new FileReader(pomFile);
        Model model = new MavenXpp3Reader().read(reader);
        model.setPomFile(pomFile);
        reader.close();
        return getMinIndexOfModules(project, model, smartListPoms, submodulePoms, minIndex);
    }

    private int getMinIndexOfModules(Project project, Model model, List<MavenModelWrapper> smartListPoms, List<MavenModelWrapper> submodulePoms, int minIndex) throws IOException, XmlPullParserException {
        for (Dependency dependency : model.getDependencies()) {
            int dependencyIndex = findIndexOfDependency(smartListPoms, submodulePoms, dependency);
            minIndex = Math.max(dependencyIndex, minIndex);
            if (dependencyIndex > -1) {
                Project dependencyProject = this.get(dependencyIndex);
                if (!dependencyProject.equals(project))
                    project.getDependencies().add(dependencyProject);
            }
        }

        for (String module : model.getModules()) {
            minIndex = Math.max(getMinIndexOfModules(project, model.getPomFile().getParentFile().toPath().resolve(module).toFile(), smartListPoms, submodulePoms, minIndex), minIndex);
        }

        return minIndex;
    }

    @SuppressWarnings("Duplicates")
    private int findIndexOfDependency(List<MavenModelWrapper> list, List<MavenModelWrapper> submodulePoms, Dependency dependencyToFind) {
        for (int i = 0; i < list.size(); i++) {
            MavenModelWrapper modelWrapper = list.get(i);
            if (modelWrapper == null)
                continue;
            Model model = modelWrapper.getModel();
            if (dependencyToFind.getGroupId().equals(model.getGroupId()) && dependencyToFind.getArtifactId().equals(model.getArtifactId()) && dependencyToFind.getVersion().equals(model.getVersion()))
                return i;
        }

        // submodules
        for (int i = 0; i < submodulePoms.size(); i++) {
            MavenModelWrapper modelWrapper = submodulePoms.get(i);
            if (modelWrapper == null)
                continue;
            Model model = modelWrapper.getModel();
            if (model.getGroupId() == null)
                model.setGroupId(model.getParent().getGroupId());
            if (model.getVersion() == null)
                model.setVersion(model.getParent().getVersion());
            if (dependencyToFind.getGroupId().equals(model.getGroupId()) && dependencyToFind.getArtifactId().equals(model.getArtifactId()) && dependencyToFind.getVersion().equals(model.getVersion()))
                return this.indexOf(modelWrapper.getParentProject());
        }

        return -1; // not found
    }

    @SuppressWarnings("Duplicates")
    private int findIndexOfParent(List<MavenModelWrapper> list, Parent modelToFind) {
        for (int i = 0; i < list.size(); i++) {
            MavenModelWrapper modelWrapper = list.get(i);
            if (modelWrapper == null)
                continue;
            Model model = modelWrapper.getModel();
            if (modelToFind.getGroupId().equals(model.getGroupId()) && modelToFind.getArtifactId().equals(model.getArtifactId()) && modelToFind.getVersion().equals(model.getVersion()))
                return i;
        }

        return -1; // not found
    }
}
