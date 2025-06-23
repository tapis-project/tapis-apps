#!/bin/bash
source ~/.bash_profile

# *************** REMOVE if copying to jenkins
# Backup of script used in Jenkins job TapisJava->3_ManualBuildDeploy->apps
# NOTE there is no guarantee that this copy in the git repo is in sync with
#      the version in use for the Jenkins job at jenkins-cic.tacc.utexas.edu
# *************** REMOVE if copying to jenkins

SVC_NAME=apps

# Might at some point move to 17.0.4 to pick up security bug fixes.
# 17.0.4-tem appears to be the latest "official" OpenJDK release as of 8-26-22
# sdk use java 17.0.4-tem
#sdk use java 17.0.2-open
sdk use java 21.0.5-tem
sdk use maven 3.6.3

java -version
mvn  -version

echo "*******************"
export TAPIS_VERSION=$(mvn org.apache.maven.plugins:maven-help-plugin:3.2.0:evaluate -Dexpression=project.version -q -DforceStdout)
echo "tapis-java TAPIS_VERSION = ${TAPIS_VERSION}"
export GIT_COMMIT=$(git log -1 --pretty=format:"%h")
export GIT_BRANCH=$GIT_BRANCH
echo "git commit revision $GIT_COMMIT"
echo "GIT_BRANCH=$GIT_BRANCH"
echo "Publish = $Publish"
echo "Deploy = $Deploy"
echo "*******************"

echo "************************ Building service: $SVC_NAME"
echo "************************ Full install: mvn clean install"
mvn clean install
RET_CODE=$?
if [ $RET_CODE -ne 0 ]; then
  echo "======================================================================"
  echo "Build of merged branch failed."
  echo "Exiting ..."
  echo "======================================================================"
  exit $RET_CODE
fi

if [ "$Deploy" == "true" -a "$Publish" == "true" ]; then
  # Set flag indicating this is a manual deploy
  # docker_build.sh script will tag image with tapis/<service_name>:dev
  export TAPIS_DEPLOY_MANUAL=true
fi

# Publish Image
echo "Publish = $Publish"
if [ "$Publish" == "true" ]; then
#	echo '************************ Running pre-publish tests'
#    export TAPIS_TENANT_SVC_BASEURL=https://master.develop.tapis.io
#    export TAPIS_DB_JDBC_URL=jdbc:postgresql://c002.rodeo.tacc.utexas.edu:?????/tapisappdb
#    release/run_integration_tests.sh
#    RET_CODE=$?
#    if [ $RET_CODE -ne 0 ]; then
#      echo "======================================================================"
#      echo "Service pre-publish test failure."
#      echo "Exiting ..."
#      echo "======================================================================"
#      exit $RET_CODE
#    fi
  cd $WORKSPACE
  echo "************************ Building & Publishing Image"
# Build agent nodes should already logged in to docker
#  docker login -u $DCKR_USERNAME -p $DCKR_PASSWD
  release/docker_build.sh dev -push
  RET_CODE=$?
  if [ $RET_CODE -ne 0 ]; then
    echo "======================================================================"
    echo "Docker build and push failure."
    echo "Exiting ..."
    echo "======================================================================"
    exit $RET_CODE
  fi
  cd $WORKSPACE
  echo "************************ Removing docker images"
  release/docker_rmi.sh dev
  RET_CODE=$?
  if [ $RET_CODE -ne 0 ]; then
    echo "======================================================================"
    echo "Docker rmi failure."
    echo "Exiting ..."
    echo "======================================================================"
    exit $RET_CODE
  fi
  cd $WORKSPACE
 fi

# Deploy Service
echo "Deploy = $Deploy"
if [ "$Deploy" == "true" ]; then
  echo "************************ Deploying Service: $SVC_NAME"
  # SSH to cic02 as the tapisdev account with access to the tapisdev k8s namespace -
  # delete pod to make it automatically pull the latest.
#  ssh -i $HOME/.ssh/Jenkins-2018 tapisdev@cic02 "cd ~/tapis-kube/${SVC_NAME}/api; ./burndown; ./burnup"
  ssh -i $HOME/.ssh/Jenkins-2018 tapisdev@129.114.35.220 "cd ~/tapis-kube/${SVC_NAME}/api; ./burndown; ./burnup"
fi
