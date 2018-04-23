#!/usr/bin/env bash
#Starts test with local Java threads
  

NAME_OF_IMAGE= #a name of a testing image file
LOCATION_OF_IMAGE= #a path to the testing image file 
PROJECT_DIRECTORY= #a path to directory with scijava-parallel project
STEP=10
COUNTS=1
NUMBER_OF_THREADS=1

#loading modules with JDK and Maven
MAVEN_OPTS=-Xmx20g
mvn test exec:java -Dexec.mainClass="test.DemonstrateParadigm" -Dexec.args="-l $COUNTS $STEP $NUMBER_OF_THREADS" -Dexec.classpathScope=test  

