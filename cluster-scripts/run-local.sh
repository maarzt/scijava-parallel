#!/usr/bin/env bash
#Starts test with local Java threads
  

NAME_OF_IMAGE= #a name of a testing image file
LOCATION_OF_IMAGE= #a path to the testing image file 
PROJECT_DIRECTORY= #a path to directory with scijava-parallel project
STEP= #size of angle step in degree 
COUNTS= #number of runs with same number of used hosts
NUMBER_OF_THREADS= #maximal number of thread used for testing

#loading modules with JDK and Maven
ml Java/1.8.0_144
ml Maven

if [ ! -d /tmp/input ] ; then
  mkdir /tmp/input
fi
if [ -d /tmp/output ] ; then
  rm -rf /tmp/output
fi
mkdir /tmp/output
if [ ! -e /tmp/input/$NAME_OF_IMAGE ] ; then
  cp $LOCATION_OF_IMAGE/$NAME_OF_IMAGE /tmp/input/
fi
cd $PROJECT_DIRECTORY
MAVEN_OPTS=-Xmx20g
mvn exec:java -Dexec.mainClass="cz.it4i.parallel.TestSuite2" -Dexec.args="-l $COUNTS $STEP $NUMBER_OF_THREADS"

