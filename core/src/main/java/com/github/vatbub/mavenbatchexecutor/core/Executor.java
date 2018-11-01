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


import org.apache.maven.shared.invoker.*;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Executor {
    @NotNull
    private ProjectList projectList;
    private boolean smartOrder;
    @NotNull
    private List<String> mavenGoals;
    private boolean executeBuildsInParallel;
    @NotNull
    private OutputSetting outputSetting;
    @Nullable
    private String mavenHome;

    public Executor(@NotNull ProjectList projectList) {
        this.projectList = projectList;
        this.smartOrder = true;
        this.mavenGoals = Collections.singletonList("install");
        this.executeBuildsInParallel = false;
        this.outputSetting = OutputSetting.TO_FILE;
    }

    @NotNull
    public ProjectList getProjectList() {
        return projectList;
    }

    public void setProjectList(@NotNull ProjectList projectList) {
        this.projectList = projectList;
    }

    public boolean isSmartOrder() {
        return smartOrder;
    }

    public void setSmartOrder(boolean smartOrder) {
        this.smartOrder = smartOrder;
    }

    @NotNull
    public List<String> getMavenGoals() {
        return mavenGoals;
    }

    public void setMavenGoals(@NotNull List<String> mavenGoals) {
        this.mavenGoals = mavenGoals;
    }

    public boolean isExecuteBuildsInParallel() {
        return executeBuildsInParallel;
    }

    public void setExecuteBuildsInParallel(boolean executeBuildsInParallel) {
        this.executeBuildsInParallel = executeBuildsInParallel;
    }

    @NotNull
    public OutputSetting getOutputSetting() {
        return outputSetting;
    }

    public void setOutputSetting(@NotNull OutputSetting outputSetting) {
        this.outputSetting = outputSetting;
    }

    @NotNull
    public List<BuildResult> executeBuilds() throws InterruptedException, IOException, XmlPullParserException {
        List<BuildResult> res = new ArrayList<>(getProjectList().size());

        ProjectList finalProjectList = isSmartOrder() ? getProjectList().getSmartOrder() : getProjectList();

        System.out.println("Build order:");
        for (Project project : finalProjectList)
            System.out.println(project.getProjectFolder());

        Map<Project, Thread> threads = new HashMap<>(finalProjectList.size());

        boolean[] setupCompleted = new boolean[1];
        setupCompleted[0] = false;

        for (Project project : finalProjectList) {
            Thread projectThread = new Thread(() -> {
                try {
                    boolean dummy=false;
                    while (!setupCompleted[0])
                        dummy = !dummy;

                    // wait for dependencies
                    for (Project dependency : project.getDependencies()) {
                        Thread dependencyThread = threads.get(dependency);
                        if (dependencyThread.isAlive()) {
                            System.out.println("(" + project.getProjectFolder() + "): Waiting for dependencies to finish building...");
                            dependencyThread.join();
                        }
                    }

                    System.out.println("(" + project.getProjectFolder() + "): Build started...");

                    InvocationRequest invocationRequest = new DefaultInvocationRequest();
                    invocationRequest.setBaseDirectory(project.getProjectFolder());
                    invocationRequest.setGoals(getMavenGoals());

                    File logFile = null;
                    if (getOutputSetting() == OutputSetting.TO_FILE) {
                        Path targetPath = project.getProjectFolder().toPath().resolve("target");
                        logFile = targetPath.resolve("batchBuildOutput.log").toFile();
                        Files.createDirectories(targetPath);
                    }

                    invocationRequest.setOutputHandler(constructOutputHandler(logFile));

                    Invoker invoker = new DefaultInvoker();
                    if (getMavenHome() != null)
                        invoker.setMavenHome(new File(getMavenHome()));
                    InvocationResult invocationResult = invoker.execute(invocationRequest);
                    res.add(new BuildResult(invocationResult, logFile));
                    System.out.println("(" + project.getProjectFolder() + "): Build finished!");
                } catch (MavenInvocationException | IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });

            threads.put(project, projectThread);
            projectThread.start();

            if (!isExecuteBuildsInParallel()) {
                setupCompleted[0] = true;
                projectThread.join();
            }
        }

        setupCompleted[0] = true;

        return res;
    }

    @NotNull
    private InvocationOutputHandler constructOutputHandler(File logFile) {
        switch (getOutputSetting()) {
            case IGNORE:
                return new IgnoreOutputHandler();
            case TO_FILE:
                if (logFile == null)
                    throw new NullPointerException("Parameter logFile must not be null");
                return new PrintToFileOutputHandler(logFile);
            case TO_STANDARD_OUT:
                return new PrintToStandardOutOutputHandler();
        }

        // should never happen
        return null;
    }

    @Nullable
    public String getMavenHome() {
        return mavenHome;
    }

    public void setMavenHome(@Nullable String mavenHome) {
        this.mavenHome = mavenHome;
    }

    public enum OutputSetting {
        IGNORE, TO_FILE, TO_STANDARD_OUT
    }

    private class IgnoreOutputHandler implements InvocationOutputHandler {

        @Override
        public void consumeLine(String s) {
            // do nothing
        }
    }

    private class PrintToStandardOutOutputHandler implements InvocationOutputHandler {

        @Override
        public void consumeLine(String s) {
            System.out.println(s);
        }
    }

    private class PrintToFileOutputHandler implements InvocationOutputHandler {
        @NotNull
        private File logFile;

        public PrintToFileOutputHandler(@NotNull File logFile) {
            this.logFile = logFile;
        }

        @Override
        public void consumeLine(String s) throws IOException {
            FileWriter fileWriter = new FileWriter(getLogFile(), true);
            fileWriter.write(s + "\n");
            fileWriter.close();
        }

        @NotNull
        public File getLogFile() {
            return logFile;
        }
    }
}
