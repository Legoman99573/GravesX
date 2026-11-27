#!/usr/bin/env bash
# Installs the bundled libs into your local Maven repository (~/.m2):
#   lib/          global libs shared by server and proxy (Libby)
#   server/libs/  server-only libs
# Run once per machine (and again if the jars in server/libs change), then build with `mvn clean package`.
# On Windows run this in Git Bash, or use build-libs.bat.
set -euo pipefail

cd "$(dirname "$0")"

install_lib() { # dir file groupId artifactId version
    if [ ! -f "$1/$2" ]; then
        echo "Missing $1/$2" >&2
        exit 1
    fi
    echo "Installing $3:$4:$5"
    mvn -B -q install:install-file -Dfile="$1/$2" -DgroupId="$3" -DartifactId="$4" -Dversion="$5" -Dpackaging=jar
}

install_lib server/libs authlib-7.0.61-graves.jar com.mojang authlib 7.0.61-graves
install_lib lib libby-2.0.1-graves2.jar com.alessiodp libby 2.0.1-graves2
install_lib server/libs playernpc-2023.6.jar dev.sergiferry PlayerNPC 2023.6
install_lib server/libs FurnitureEngine-3.3.jar com.mira furnitureengine 3.3

echo "Done. Now run: mvn clean package"
