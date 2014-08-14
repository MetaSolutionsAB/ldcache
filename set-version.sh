#!/bin/bash

if [ $# -eq 0 ]
then
  echo "Usage: $0 {version}"
  exit
fi

mvn versions:set -DnewVersion=$1
echo $1 > VERSION.txt
echo $1 > webapp/src/main/resources/VERSION.txt
