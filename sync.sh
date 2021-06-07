#!/bin/env/bash

rsync -rpvtz --delete ./ carlos@192.168.0.30:/home/carlos/Proyectos/java/sonar/
