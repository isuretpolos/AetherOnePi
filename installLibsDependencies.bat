call mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file -Dfile=libs/controlP5.jar -DgroupId=sojamo.de -DartifactId=controlP5 -Dversion=2.1.6 -Dpackaging=jar
call mvn org.apache.maven.plugins:maven-install-plugin:2.3.1:install-file -Dfile=libs/apple.jar -DgroupId=processing.org -DartifactId=appleJar -Dversion=1.0.0 -Dpackaging=jar

pause
