#!/bin/bash
cd "$(dirname "$0")"
mvn -q exec:java -Dexec.mainClass="equations_from_jsons" -Dexec.args="$1" -Dexec.cleanupDaemonThreads=false
