#!/usr/bin/env bash

#This script distribute ImageJ server on every node allocated for current job.
pbsdsh `readlink -f run-imagej-server.sh`
