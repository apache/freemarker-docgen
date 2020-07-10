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
package org.freemarker.docgen.ant;

import java.io.File;
import java.util.TimeZone;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.freemarker.docgen.core.Transform;

/**
 * Ant task "interface" to {@link Transform}.
 * 
 * <p>Usage example:
 *<pre>
&lt;project ... xmlns:docgen="http://freemarker.org/docgen"&gt;

  &lt;taskdef resource="org/freemarker/docgen/antlib.properties"
      uri="http://freemarker.org/docgen"
      classpath="<i>DOCGEN_HOME</i>/lib/docgen.jar"
  /&gt;

  &lt;target ...&gt;
    &lt;!-- If you want to ensure that the destination directory is empty: --&gt;
    &lt;mkdir dir="<i>DEST_DIR</i>" /&gt;
    &lt;delete includeEmptyDirs="true"&gt;
      &lt;fileset dir="<i>DEST_DIR</i>" includes="**<!--
          -->/*" defaultexcludes="no" /&gt;
    &lt;/delete&gt;
    
    <b>&lt;docgen:transform <!--
           -->srcdir="<i>SRC_DIR</i>" destdir="<i>DEST_DIR</i>" offline="<i>OFFLINE</i>" /&gt;</b>
  &lt;/target&gt;

&lt;/project&gt;
</pre>
 */
public final class TransformAntTask extends Task {

    private Transform transform = new Transform();
    {
        transform.setPrintProgress(true);
    }
    
    public void setDestDir(File value) {
        transform.setDestinationDirectory(value.getAbsoluteFile());
    }
    
    public void setSrcDir(File value) {
        transform.setSourceDirectory(value);
    }
    
    public void setShowEditoralNotes(boolean value) {
        transform.setShowEditoralNotes(value);
    }
    
    public void setValidate(boolean value) {
        transform.setValidate(value);
    }
    
    public void setTimeZone(String timeZone) {
        transform.setTimeZone(TimeZone.getTimeZone(timeZone));
    }

    public void setGenerateEclipseToC(boolean value) {
        transform.setGenerateEclipseToC(value);
    }
    
    public Boolean getOffline() {
        return transform.getOffline();
    }

    public void setOffline(Boolean offline) {
        transform.setOffline(offline);
    }
    
    @Override
    public void execute() {
        try {
            transform.execute();
        } catch (Exception e) {
            e.printStackTrace();
            throw new BuildException(e);
        }
    }
    
}
