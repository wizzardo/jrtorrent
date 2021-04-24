#!/usr/bin/env bash

./gradlew clean fatJar

scp build/libs/jrtorrent-all-0.1.jar home2:/tmp/uploads/jrt.jar_ && ssh home2 mv /tmp/uploads/jrt.jar_ /tmp/uploads/jrt.jar