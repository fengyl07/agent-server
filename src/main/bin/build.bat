@echo on
cd src/main/frontend
@echo "start build"
yarn install & yarn build
@echo "success build"
