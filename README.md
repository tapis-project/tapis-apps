# tapis-apps

Tapis Applications Service

There are three primary branches: *local*, *dev*, and *main*.

All changes should first be made in the branch *local*.

When it is time to deploy to the **DEV** kubernetes environment
run the jenkins job TapisJava->3_ManualBuildDeploy->apps.

This job will:
* Merge changes from *local* to *dev*
* Build, tag and push docker images
* Deploy to **DEV** environment
* Push the merged *local* changes to the *dev* branch.
