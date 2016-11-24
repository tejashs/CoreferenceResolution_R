#!/bin/sh
listOfFile="$1"
directory="$2"
echo ListofFilesPathGiven : $listOfFile
echo Output Directory Given : $directory
mvn clean compile assembly:single
java -jar target/*.jar $listOfFile $directory
