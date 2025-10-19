#!/bin/bash
cd "$(dirname "$0")"
mvn -q exec:java -Dexec.mainClass="equations_from_json" -Dexec.args="$1" -Dexec.cleanupDaemonThreads=false
