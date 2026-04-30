#!/usr/bin/env bash

sbt -Dplay.http.router=testOnlyDoNotUseInAppConf.Routes -Dclock-module=testOnly.config.Module run
