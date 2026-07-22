#!/bin/sh
# Run Spotless via the Maven wrapper so a system `mvn` install isn't required.
# Falls back to `mvn` if the wrapper is missing.
cd "$(dirname "$0")" || exit 1
if [ -x ./mvnw ]; then
    ./mvnw spotless:apply
else
    mvn spotless:apply
fi
