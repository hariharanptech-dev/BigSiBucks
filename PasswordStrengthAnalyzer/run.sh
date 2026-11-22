#!/usr/bin/env bash
# Compiles the project into ./out and starts the application.
set -e
cd "$(dirname "$0")"
mkdir -p out
javac -d out src/*.java
java -cp out Main
