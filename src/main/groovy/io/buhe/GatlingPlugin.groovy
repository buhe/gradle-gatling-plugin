package io.buhe

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.process.ExecResult

import java.util.logging.Level
import java.util.logging.Logger
import java.util.regex.Pattern


class GatlingPlugin implements Plugin<Project> {

    Pattern whiteList;
    Pattern blackList;

    private static final Logger log = Logger.getLogger(GatlingPlugin.class.toString());

    private Pattern createPatternFromList(List<String> list) {
        return Pattern.compile(list.join("|"));
    }

    private boolean check(String fullyQualifiedName){
        if (fullyQualifiedName.contains("\$")) {
            log.log(Level.FINE, "Skipping ${fullyQualifiedName} as it does not appear to be a simulation")
            return false;
        } else if (whiteList.matcher(fullyQualifiedName).matches()) {
            log.log(Level.FINE, "${fullyQualifiedName} in white list");
            return true;
        } else if (blackList.matcher(fullyQualifiedName).matches()) {
            log.log(Level.FINE, "${fullyQualifiedName} in black list");
            return false;
        } else {
            log.log(Level.FINE, "${fullyQualifiedName} not in neither black not white list")
            return true;
        }
    }

    private String getFullyQualifiedName(File file, Project project) {
        return (file.getPath() - (project.sourceSets.test.output.classesDir.getPath() + File.separator) - '.class')
                .replace(File.separator, ".")
    }

    private String getReportDirectory(Project project) {
        return project.buildDir.getAbsolutePath() + "/reports/gatling";
    }

    void apply(Project project) {

        project.extensions.create("gatling", GatlingPluginExtension);

        project.task('gatling').dependsOn("compileTestScala") << {
            log.log(Level.FINE, 'Include ${project.gatling.include}')
            log.log(Level.FINE, 'Exclude ${project.gatling.exclude}')

            def include = Arrays.asList(System.getProperty('gatling.include', project.gatling.include).split(','))
            def exclude = Arrays.asList(System.getProperty('gatling.exclude', project.gatling.exclude).split(','))
            def failedSimulations = []
            def breakOnFailure = Boolean.valueOf(System.getProperty('gatling.breakOnFailure', project.gatling.breakOnFailure as String))
            def ignoreFailures = Boolean.valueOf(System.getProperty('gatling.ignoreFailures', project.gatling.ignoreFailures as String))
            ExecResult execResult = null

            whiteList = createPatternFromList(include);
            blackList = createPatternFromList(exclude);

            logger.lifecycle(" ---- Executing all Gatling scenarios from: ${project.sourceSets.test.output.classesDir} ----")
            project.sourceSets.test.output.classesDir.eachFileRecurse { file ->
                if (file.isFile()) {
                    //Remove the full path, .class and replace / with a .
                    logger.debug("Tranformed file ${file} into")
                    def gatlingScenarioClass = getFullyQualifiedName(file, project);
                    if (check(gatlingScenarioClass)) {
                        logger.debug("Tranformed file ${file} into scenario class ${gatlingScenarioClass}")

                        execResult = project.javaexec {
                            main = 'io.gatling.app.Gatling'
                            classpath = project.sourceSets.test.output + project.sourceSets.test.runtimeClasspath
                            systemProperties = project.gatling.systemProperties
                            ignoreExitValue = true
                            args  '-bf',
                                    project.sourceSets.test.output.classesDir,
                                    '-s',
                                    gatlingScenarioClass,
                                    '-rf',
                                    getReportDirectory(project)
                        }

                       if (execResult.exitValue != 0) {
                            logger.warn("Execution of test ${gatlingScenarioClass} failed".toString())
                            failedSimulations.add(gatlingScenarioClass)
                            if (breakOnFailure) {
                                execResult.assertNormalExitValue()
                            }
                        }
                    }
                }
            }

            logger.lifecycle(" ---- Done executing all Gatling scenarios ----")
            if (!ignoreFailures && failedSimulations.size() > 0) {
                throw new GroovyRuntimeException("The following tests failed: ${failedSimulations}".toString())
            }
        }
    }
}

