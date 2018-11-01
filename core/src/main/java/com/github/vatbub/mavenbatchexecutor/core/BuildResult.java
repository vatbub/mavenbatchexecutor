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


import org.apache.maven.shared.invoker.InvocationResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public class BuildResult {
    @NotNull
    private InvocationResult invocationResult;
    @Nullable
    private File logOutputFile;

    public BuildResult(@NotNull InvocationResult invocationResult, @Nullable File logOutputFile) {
        this.invocationResult = invocationResult;
        this.logOutputFile = logOutputFile;
    }

    @NotNull
    public InvocationResult getInvocationResult() {
        return invocationResult;
    }

    public void setInvocationResult(@NotNull InvocationResult invocationResult) {
        this.invocationResult = invocationResult;
    }

    @Nullable
    public File getLogOutputFile() {
        return logOutputFile;
    }

    public void setLogOutputFile(@Nullable File logOutputFile) {
        this.logOutputFile = logOutputFile;
    }
}
