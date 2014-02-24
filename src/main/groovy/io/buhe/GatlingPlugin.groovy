package io.buhe;

import org.gradle.api.Plugin
import org.gradle.api.Project

class GatlingPlugin implements Plugin<Project> {
    void apply(Project project) {

        project.task('gatling').dependsOn("compileTestScala") << {
            println "Current time is " + new Date();


            logger.lifecycle(" ---- Executing all Gatling scenarios from: ${project.sourceSets.test.output.classesDir} ----")
            project.sourceSets.test.output.classesDir.eachFileRecurse { file ->
                if (file.isFile()) {
                    //Remove the full path, .class and replace / with a .
                    logger.debug("Tranformed file ${file} into")
                    def gatlingScenarioClass = (file.getPath() - (project.sourceSets.test.output.classesDir.getPath() + File.separator) - '.class')
                            .replace(File.separator, '.')

                    logger.debug("Tranformed file ${file} into scenario class ${gatlingScenarioClass}")
                    project.javaexec {
                        // I do not use this so
                        main = 'com.excilys.ebi.gatling.app.Gatling'
                        classpath = project.sourceSets.test.output + project.sourceSets.test.runtimeClasspath
                        report = project.buildDir.getAbsolutePath()+'/reports/gatling';
                        args  '-sbf',
                                project.sourceSets.test.output.classesDir,
                                '-s',
                                gatlingScenarioClass,
                                '-rf',
                                report
                    }
                }
            }

            logger.lifecycle(" ---- Done executing all Gatling scenarios ----")

        }
    }
}