package org.freemarker.docgen;

import java.io.File;
import java.util.TimeZone;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;

/**
 * Ant task "interface" to {@link Transform}.
 * 
 * <p>Usage example:
 *<pre>
&lt;project ... xmlns:docgen="http://freemarker.org/docgen">

  &lt;taskdef resource="org/freemarker/docgen/antlib.properties"
      uri="http://freemarker.org/docgen"
      classpath="<i>DOCGEN_HOME</i>/lib/docgen.jar"
  />

  &lt;target ...>
    &lt;!-- If you want to ensure that the destination directory is empty: -->
    &lt;mkdir dir="<i>DEST_DIR</i>" />
    &lt;delete includeEmptyDirs="true">
      &lt;fileset dir="<i>DEST_DIR</i>" includes="**<!--
          -->/*" defaultexcludes="no" />
    &lt;/delete>
    
    <b>&lt;docgen:transform <!--
           -->srcdir="<i>SRC_DIR</i>" destdir="<i>DEST_DIR</i>" offline="<i>OFFLINE</i>" /&gt;</b>
  &lt;/target>

&lt;/project>
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
