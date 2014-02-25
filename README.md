# Install
======================

execute:

	gradle uploadArchives

put lib directory into your project directory,add configuration to your build.gradle

	buildscript {
	    repositories {
	        maven {
	            url 'file:lib'
	        }
	    }
	    dependencies {
	        classpath 'io.buhe.gradle:gatling:0.0.1'
	    }
	}

# Configuration
======================

	apply plugin: 'gatling'
    gatling {
        exclude ('^basic.*')
        include ('^basic.Github.*')
    }


exclude is black list,include is white list,support regx.

#Tasks
======================

	:gatling     execute all match gatling script.