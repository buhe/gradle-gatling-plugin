package io.buhe;

import org.gradle.api.Plugin
import org.gradle.api.Project

import java.util.regex.Pattern

class GatlingPlugin implements Plugin<Project> {

    static Pattern createPatternFromList(List<String> list) {
        if (list == null || list.size() == 0)
            return null;
        StringBuilder sb = new StringBuilder(500);
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1)
                sb.append("|");
        }
        return Pattern.compile(sb.toString());
    }

    static boolean check(String clazz,Pattern blackListPattern,Pattern whiteListPattern){
        if (inWhiteList(clazz,whiteListPattern)) {
            println "---- "+clazz + "  in white list. ----"
            return true;
        } else {
            if (inBlackList(clazz,blackListPattern)) {
                println "---- "+clazz + "  in black list. ----"
                return false;
            } else {
                println "---- "+clazz + "  not in black and white list. ----"
                return true;
            }
        }
    }

    static  boolean inBlackList(String path, Pattern blackListPattern) {
        if (blackListPattern == null)
            return false;
        return blackListPattern.matcher(path).matches();
    }

    static boolean inWhiteList(String path, Pattern whiteListPattern) {
        if (whiteListPattern == null)
            return false;
        return whiteListPattern.matcher(path).matches();
    }


    void apply(Project project) {

        project.extensions.create("gatling", GatlingPluginExtension);

        project.task('gatling').dependsOn("compileTestScala") << {
            println 'Include ' + project.gatling.include
            println 'Exclude ' + project.gatling.exclude

            def whiteListPattern = createPatternFromList(Arrays.asList(project.gatling.include));
            def blackListPattern = createPatternFromList(Arrays.asList(project.gatling.exclude));

            println 'Include ' + whiteListPattern
            println 'Exclude ' + blackListPattern

            logger.lifecycle(" ---- Executing all Gatling scenarios from: ${project.sourceSets.test.output.classesDir} ----")
            project.sourceSets.test.output.classesDir.eachFileRecurse { file ->
                if (file.isFile()) {
                    //Remove the full path, .class and replace / with a .
                    logger.debug("Tranformed file ${file} into")
                    def gatlingScenarioClass = (file.getPath() - (project.sourceSets.test.output.classesDir.getPath() + File.separator) - '.class')
                            .replace(File.separator, '.')
                    if(check(gatlingScenarioClass,blackListPattern,whiteListPattern)){
                        logger.debug("Tranformed file ${file} into scenario class ${gatlingScenarioClass}")
                        def report = project.buildDir.getAbsolutePath()+'/reports/gatling';
                        project.javaexec {
                            // I do not use this so
                            main = 'io.gatling.app.Gatling'
                            classpath = project.sourceSets.test.output + project.sourceSets.test.runtimeClasspath
                            args  '-bf',
                                    project.sourceSets.test.output.classesDir,
                                    '-s',
                                    gatlingScenarioClass,
                                    '-rf',
                                    report
                        }
                    }


                }
            }

            logger.lifecycle(" ---- Done executing all Gatling scenarios ----")
        }
    }
}

