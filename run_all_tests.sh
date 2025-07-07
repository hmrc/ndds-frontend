#!/usr/bin/env bash

sbt clean coverage test coverageOff coverageReport
