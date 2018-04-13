#!/usr/bin/env bash

PROJECT_DIRECTORY= #a path to directory with imagej-server project
#environment settings needed for ImageJ server run
EBROOTJAVA=/apps/all/Java/1.8.0_144
LIBRARY_PATH=/apps/all/Java/1.8.0_144/lib:/apps/all/Maven/3.3.9/lib
LMOD_PACKAGE_PATH=/apps/all/Lmod/7.7.7/lmod/etc
LMOD_PKG=/apps/all/Lmod/7.7.7/lmod/lmod
PBS_ENVIRONMENT=PBS_INTERACTIVE
EBVERSIONMAVEN=3.3.9
QTDIR=/usr/lib64/qt-3.3
LMOD_VERSION=7.7.7
QTINC=/usr/lib64/qt-3.3/include
NCPUS=24
PBS_TASKNUM=1
LD_LIBRARY_PATH=/apps/all/Java/1.8.0_144/lib:/apps/all/Maven/3.3.9/lib:/apps/all/Lua/5.1.4-8/lib
LMOD_sys=Linux
PATH=/apps/all/Java/1.8.0_144:/apps/all/Java/1.8.0_144/bin:/apps/all/Maven/3.3.9:/apps/all/Maven/3.3.9/bin:/opt/sgi/sbin:/opt/sgi/bin:/usr/lib64/qt-3.3/bin:/bin:/usr/bin:/usr/local/sbin:/usr/sbin:/sbin:/opt/c3/bin:/opt/pbs/default/bin
JAVA_HOME=/apps/all/Java/1.8.0_144
cd $PROJECT_DIRECTORY
MAVEN_OPTS=-Xmx10g
mvn exec:java -Dexec.mainClass="net.imagej.server.Main" 
