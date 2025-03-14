# WE WILL NOT PROVIDE SUPPORT FOR BUILDING ANY CUSTOM VERSIONS OF GRAVES. THIS IS HERE FOR THOSE WHO WANT TO MAKE THEIR OWN VERSIONS
these files will need to be manually installed into maven using the following command from inside the libs folder (on windows use git bash or similar not cmd.exe):
```shell
mvn install:install-file -Dfile=libs/authlib-6.0.55-graves.jar -DgroupId=com.mojang -DartifactId=authlib -Dversion=6.0.55-graves -Dpackaging=jar
mvn install:install-file -Dfile=libs/libby-2.0.1-graves.jar -DgroupId=com.alessiodp -DartifactId=libby -Dversion=2.0.1-graves -Dpackaging=jar
mvn install:install-file -Dfile=libs/playernpc-2023.6.jar -DgroupId=dev.sergiferry -DartifactId=PlayerNPC -Dversion=2023.6 -Dpackaging=jar
mvn install:install-file -Dfile=libs/FurnitureEngine-3.3.jar -DgroupId=com.mira -DartifactId=furnitureengine -Dversion=3.3 -Dpackaging=jar
```
files that include "-graves" are modified versions of libs that are specifically made to make maintenance of graves more streamlined