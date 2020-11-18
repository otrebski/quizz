#!/bin/bash
#Script for local building
echo "Building app"
docker build -t quizz -f Dockerfile .
echo "Done"
