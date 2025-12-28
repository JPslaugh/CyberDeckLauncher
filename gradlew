#!/bin/sh

##############################################################################
# Gradle startup script for POSIX
##############################################################################

# Attempt to set APP_HOME
SAVED="`pwd`"
cd "`dirname \"$0\"`"
APP_HOME="`pwd -P`"
cd "$SAVED"

# Resolve links: $0 may be a link
PRG="$0"
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done

CLASSPATH=$APP_HOME/gradle/wrapper/gradle-wrapper.jar

exec java -classpath "$CLASSPATH" org.gradle.wrapper.GradleWrapperMain "$@"
