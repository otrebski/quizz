#!/bin/bash
#Script for local building
echo "Building app"
docker build -t decision-tree -f Dockerfile .
echo "Done"
