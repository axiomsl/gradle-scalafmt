package cz.alenkacz.gradle.scalafmt

import org.gradle.api.DefaultTask
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.SourceSet
import org.scalafmt.interfaces.Scalafmt

class ScalafmtFormatBase extends DefaultTask {
    SourceSet sourceSet
//    ClassLoader cl = this.class.getClassLoader()
    PluginExtension pluginExtension
    static def formatter = Scalafmt.create(ScalafmtFormatBase.classLoader)
            .withRespectVersion(false)
            .withDefaultVersion("1.5.1")

    def runScalafmt(boolean testOnly = false) {
        if (project.plugins.withType(JavaBasePlugin).empty) {
            logger.info("Java or Scala gradle plugin not available in this project, nothing to format")
            return
        }
        def configpath = ConfigFactory.get(logger, project, pluginExtension.configFilePath)
        def misformattedFiles = new ArrayList<String>()

        def dir = this.getTemporaryDir()
        def scalafmtLastrun = new File(dir, 'scalafmt-lastrun')
        Long lastRunMillis = 0L
        if (scalafmtLastrun.exists()) {
            lastRunMillis = scalafmtLastrun.text.toLong()
        }

        sourceSet.allSource
                .filter { File f -> f.lastModified() == 0 || f.lastModified() > lastRunMillis }
                .filter { File f -> canBeFormatted(f) }
                .each { File f ->
                    String contents = f.text
                    logger.debug("Formatting '$f'")
                    def formattedContents = formatter.format(configpath.toPath(), f.toPath(), contents)
                    if (contents != formattedContents) {
                        if (testOnly) {
                            misformattedFiles.add(f.absolutePath)
                        } else {
                            f.write(formattedContents, "UTF-8")
                        }
                    }
                }

        if (testOnly && !misformattedFiles.empty) {
            throw new ScalafmtFormatException(misformattedFiles)
        }
        new File(dir, 'scalafmt-lastrun').withWriter { w ->
            w << System.currentTimeMillis()
        }
    }

    static boolean canBeFormatted(File file) {
        file.getAbsolutePath().endsWith(".scala") || file.getAbsolutePath().endsWith(".sbt")
    }
}
