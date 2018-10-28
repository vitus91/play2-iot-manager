#!/usr/bin/env bash

# for k8s or minikube better change Module.scala and build.sbt back to origin values
# DockerPlugin does not minify your image, so it's huge. If you use SbtReactivePlugin it'll minify your image

./run_build_final.sh
docker-compose up

