#!/usr/bin/env bash
#Starts test with ImageJ server
  
PBS_JOBID=$1 #use an id of job in which instances of ImageJ server run 
NAME_OF_IMAGE= #a name of a testing image file
LOCATION_OF_IMAGE= #a path to the testing image file 
PROJECT_DIRECTORY= #a path to directory with scijava-parallel project
STEP= #size of angle step in degree 
COUNTS= #number of runs with same number of used hosts

#loading modules with JDK and Maven
ml Java/1.8.0_144
ml Maven


PBS_JOBID=$1
HOSTS="`qstat -f $PBS_JOBID | tr -d '\n' | sed 's/24\t+r/24+r/g' | sed 's/ = /=/g' | sed 's/\s\+/\n/g' | grep exec_host | sed 's/exec_host=//g'`"
HOSTS=$(echo $HOSTS| sed "s/ //g" | sed "s/\/0\*24//g" | sed "s/[+]/ /g")

echo $HOSTS
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
MAVEN_OPTS=-Xmx2g
mvn exec:java -Dexec.mainClass="cz.it4i.parallel.TestSuite2" -Dexec.args="$COUNTS $STEP $HOSTS"

