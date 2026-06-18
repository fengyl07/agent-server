#!/bin/sh
cd src/main/frontend
source /etc/profile
echo "start build"
yarn install && yarn build
echo "success build"