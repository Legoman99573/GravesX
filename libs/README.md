# WE WILL NOT PROVIDE SUPPORT FOR BUILDING ANY CUSTOM VERSIONS OF GRAVES. THIS IS HERE FOR THOSE WHO WANT TO MAKE THEIR OWN VERSIONS
this file will need to be manually installed into maven using the following command from inside the libs folder(I recommend using git bash):
mvn install:install-file -Dfile=libs/authlib-6.0.55-graves.jar -DgroupId=com.mojang -DartifactId=authlib -Dversion=6.0.55-graves -Dpackaging=jar  
this file is a modified version of authlib that specifically allows backwards compatibility with older method naming(versions before 1.20.2)