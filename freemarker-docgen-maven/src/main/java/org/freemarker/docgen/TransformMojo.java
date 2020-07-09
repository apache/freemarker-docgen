/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.freemarker.docgen;

import java.io.File;
import java.util.TimeZone;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "transform")
public class TransformMojo extends AbstractMojo {

    @Parameter(required = true)
    private File sourceDirectory;

    @Parameter(required = true)
    private File outputDirectory;

    @Parameter
    private Boolean showEditoralNotes;

    @Parameter
    private Boolean validate;

    @Parameter
    private String timeZone;

    @Parameter
    private Boolean generateEclipseToC;

    @Parameter(required = true)
    private Boolean offline;

    @Parameter()
    private boolean printProgress = true;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Transform transform = new Transform();
        transform.setSourceDirectory(sourceDirectory);
        transform.setDestinationDirectory(outputDirectory);
        if (showEditoralNotes != null) {
            transform.setShowEditoralNotes(showEditoralNotes);
        }
        if (validate != null) {
            transform.setValidate(validate);
        }
        if (timeZone != null) {
            transform.setTimeZone(TimeZone.getTimeZone(timeZone));
        }
        if (showEditoralNotes != null) {
            transform.setShowEditoralNotes(showEditoralNotes);
        }
        if (validate != null) {
            transform.setValidate(validate);
        }
        transform.setPrintProgress(printProgress); // TODO Use Maven logging for this
        try {
            transform.execute();
        } catch (Exception e) {
            throw new MojoExecutionException("Error during document transformation", e);
        }
    }
}
