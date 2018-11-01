package com.github.vatbub.mavenbatchexecutor.cli;

/*-
 * #%L
 * maven-batch-executor.cli
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


import com.github.vatbub.mavenbatchexecutor.core.BuildResult;
import com.github.vatbub.mavenbatchexecutor.core.Executor;
import com.github.vatbub.mavenbatchexecutor.core.Project;
import com.github.vatbub.mavenbatchexecutor.core.ProjectList;
import org.apache.commons.cli.*;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Main {
    static Options options;
    private static Option parentFolderOption;
    private static Option projectListOption;
    private static Option smartOrderOption;
    private static Option mavenGoalsOption;
    private static Option executeBuildsInParallelOption;
    private static Option outputSettingOption;
    private static Option mavenHomeOption;

    public static void main(String[] args) throws ParseException, InterruptedException, XmlPullParserException, IOException {
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = parser.parse(getOptions(), args);

        ProjectList projectList = null;

        if (commandLine.hasOption(getParentFolderOption().getOpt())) {
            String parentFolderAsString = commandLine.getOptionValue(getParentFolderOption().getOpt());
            File parentFolder = new File(parentFolderAsString);

            FileFilter directoryFileFilter = File::isDirectory;

            File[] subfolders = parentFolder.listFiles(directoryFileFilter);

            if (subfolders == null)
                throw new NullPointerException("The specified parent folder does not contain any subfolders!");

            projectList = new ProjectList(subfolders.length);
            for (File subfolder : subfolders) {
                projectList.add(new Project(subfolder));
            }
        }


        if (commandLine.hasOption(getProjectListOption().getOpt())) {
            String projectListString = commandLine.getOptionValue(getProjectListOption().getOpt());
            String[] projectArray = projectListString.split(";");
            projectList = new ProjectList(projectArray.length);
            for (String project : projectArray) {
                projectList.add(new Project(project));
            }
        }

        if (projectList == null)
            throw new NullPointerException("Either pf or pl must be specified!");

        Executor executor = new Executor(projectList);

        if (commandLine.hasOption(getSmartOrderOption().getOpt()))
            executor.setSmartOrder(true);

        String mavenGoalsString = commandLine.getOptionValue(getMavenGoalsOption().getOpt());
        String[] goalArray = mavenGoalsString.split(";");
        List<String> goalsList = new ArrayList<>(Arrays.asList(goalArray));

        executor.setMavenGoals(goalsList);

        if (commandLine.hasOption(getExecuteBuildsInParallelOption().getOpt()))
            executor.setExecuteBuildsInParallel(true);

        if (commandLine.hasOption(getOutputSettingOption().getOpt())) {
            Executor.OutputSetting outputSetting = Executor.OutputSetting.valueOf(commandLine.getOptionValue(getOutputSettingOption().getOpt()));
            executor.setOutputSetting(outputSetting);
        }

        if (commandLine.hasOption(getMavenHomeOption().getOpt())) {
            String mavenHome = commandLine.getOptionValue(getMavenHomeOption().getOpt());
            executor.setMavenHome(mavenHome);
        }

        List<BuildResult> buildResults = executor.executeBuilds();
        for (BuildResult buildResult : buildResults) {
            if (buildResult.getLogOutputFile() != null)
                System.out.println("Build log saved in: " + buildResult.getLogOutputFile().getAbsolutePath());
        }
    }

    public static Options getOptions() {
        if (options == null) {
            options = new Options();
            options.addOption(getParentFolderOption());
            options.addOption(getProjectListOption());
            options.addOption(getSmartOrderOption());
            options.addOption(getMavenGoalsOption());
            options.addOption(getExecuteBuildsInParallelOption());
            options.addOption(getOutputSettingOption());
            options.addOption(getMavenHomeOption());
        }
        return options;
    }

    public static Option getParentFolderOption() {
        if (parentFolderOption == null) {
            parentFolderOption = new Option("pf", "parentFolder", true, "The parent folder which contains all projects to be build. Note: Each of the subfolders must contain a pom file. Folders which do not contain a pom file will cause that particular build to fail.");
            parentFolderOption.setRequired(false);
        }

        return parentFolderOption;
    }

    public static Option getProjectListOption() {
        if (projectListOption == null) {
            projectListOption = new Option("pl", "projectList", true, "The list of projects to build. The projects are specified by specifying their base path (the path where the pom is in). Multiple projects must be separated through a semicolon (;)");
            projectListOption.setRequired(false);
        }

        return projectListOption;
    }

    public static Option getSmartOrderOption() {
        if (smartOrderOption == null) {
            smartOrderOption = new Option("so", "smartOrder", false, "If specified, the executor will try to order the builds in such a way that required dependencies will be built first. NOTE: Contrary to Maven's Build Reactor, builds will not have access to the artifacts produced by other builds. It is therefore recommended to use the install goal to install artifacts to the local maven repository.");
            smartOrderOption.setRequired(false);
        }

        return smartOrderOption;
    }

    public static Option getMavenGoalsOption() {
        if (mavenGoalsOption == null) {
            mavenGoalsOption = new Option("g", "goals", true, "The list of goals to execute. All specified goals will be executed in all projects. Multiple goals must be separated through a semicolon (;)");
            mavenGoalsOption.setRequired(true);
        }

        return mavenGoalsOption;
    }

    public static Option getExecuteBuildsInParallelOption() {
        if (executeBuildsInParallelOption == null) {
            executeBuildsInParallelOption = new Option("parallel", "executeInParallel", false, "If specified, builds will be executed in parallel if possible.");
            executeBuildsInParallelOption.setRequired(false);
        }

        return executeBuildsInParallelOption;
    }

    public static Option getOutputSettingOption() {
        if (outputSettingOption == null) {
            outputSettingOption = new Option("out", "outputSetting", true, "Specifies how the build log is treated. Possible values are: TO_FILE (default), TO_STANDARD_OUT and IGNORE");
            outputSettingOption.setRequired(false);
        }

        return outputSettingOption;
    }

    public static Option getMavenHomeOption() {
        if (mavenHomeOption == null) {
            mavenHomeOption = new Option("mh", "mavenHome", true, "The maven home directory");
            mavenHomeOption.setRequired(false);
        }

        return mavenHomeOption;
    }
}
