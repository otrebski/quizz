#!/bin/bash
#Script for local building
echo "Building gui"
docker build -t quizz-gui -f gui/Dockerfile .
echo "Building backend"
docker build -t quizz -f Dockerfile .
echo "Done"
