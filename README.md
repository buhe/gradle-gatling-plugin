# Install

1. Execute:

	gradle install

2. Add the following to your `build.gradle`:

	buildscript {
		dependencies {
			classpath 'io.buhe.gradle:gatling:0.0.1'
		}
	}


# Configuration

	apply plugin: 'gatling'

	gatling {
		exclude '^basic.*'
		include '^basic.Github.*'
	}


exclude is a black list, include is a white list.  Both support regex.


#Tasks

	:gatling	 execute all matching gatling scripts

