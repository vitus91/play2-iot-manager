#!/bin/bash

export DATABASE_PORT="5555"
export DATABASE_DB="build"

# You need a postgres docker instance and wait for it like in your test script.

sbt flyway/flywayMigrate
sbt docker:publishLocal

# Clean up your docker instances.