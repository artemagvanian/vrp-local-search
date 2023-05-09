#!/bin/bash

########################################
############# CSCI 2951-O ##############
########################################

# Update this file with instructions on how to compile your code
javac -classpath /local/projects/cplex/CPLEX_Studio221/cplex/lib/cplex.jar \
    ./src/solver/ls/*.java \
    ./src/solver/ls/data/*.java \
    ./src/solver/ls/incremental/*.java \
    ./src/solver/ls/instances/*.java \
    ./src/solver/ls/interchanges/*.java \
    ./src/solver/ls/utils/*.java
