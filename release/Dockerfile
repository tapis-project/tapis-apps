##########################################################################
# Dockerfile for Tapis Applications service
# This file must be placed in the build directory (api/target)
#   before docker build is run.
#
#   $TAG            the tag for image identification
#
##########################################################################
FROM openjdk:17-jdk-bullseye

MAINTAINER CIC Support <cicsupport@tacc.utexas.edu>

WORKDIR /app

# Copy jar file into place
COPY "apps.jar" .
COPY /dependencies dependencies


# Environment variables for testing without JWT
# ENV TAPIS_ENVONLY_ALLOW_TEST_HEADER_PARMS true
# ENV TAPIS_ENVONLY_JWT_OPTIONAL true
# ENV TAPIS_ENVONLY_SKIP_JWT_VERIFY true

# Other environment variables
ENV TAPIS_REQUEST_LOGGING_FILTER_PREFIXES /v3/apps

# Java module commands that are always needed (java 17 and later).
ENV JAVA_MODULE_OPTS --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.time=ALL-UNNAMED \
                     --add-opens java.base/java.util=ALL-UNNAMED

# Options for JVM
ENV JAVA_TOOL_OPTIONS -agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=n \
                      -Djava.rmi.server.hostname=0.0.0.0 -Dcom.sun.management.jmxremote \
                      -Dcom.sun.management.jmxremote.port=9090  -Dcom.sun.management.jmxremote.rmi.port=9090 \
                      -Dcom.sun.management.jmxremote.local.only=false -Dcom.sun.management.jmxremote.authenticate=false \
                      -Dcom.sun.management.jmxremote.ssl=false \
                      -Dp6spy.config.modulelist=
# ENV JAVA_TOOL_OPTIONS -Dp6spy.config.modulelist=

# Server port, debug port and jmx port
EXPOSE 8080 8000 9090

CMD java -Xms4g -Xmx4g ${JAVA_MODULE_OPTS} -cp "apps.jar:dependencies/*" edu.utexas.tacc.tapis.apps.api.AppsApplication
