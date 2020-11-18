#!/bin/bash
#Script for local building
echo "Building app"
docker build -t decisiontree -f Dockerfile .
echo "Done"
