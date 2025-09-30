#!/usr/bin/env bash

sbt compile coverage test coverageReport dependencyUpdates
