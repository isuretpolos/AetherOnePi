# Documentation
## Quick Guide
TODO work in progress
## Developer Guide
Note: I will not give support to the novice in java programming.
I assume that you are a professional developer if you read this chapter.
I rather concentrate my time in research, developing and writing about radionics.
It is not my task to teach you programming (even if I would love to ... believe me!).

### Requirements
The following requirements are also the skills you need to master for partecipating to the project. Note that these are well known industrial standards.
But if you want to just build your own release, basic knowledge should be sufficient.

- [Git](https://git-scm.com/downloads)
- [Java 8](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
- [Maven 3](https://maven.apache.org/download.cgi)
- [NodeJS](https://nodejs.org/en/download/)
- [Chocolatey](https://chocolatey.org/) ... for Windows only (other OS use their own package manager)
- [Angular CLI](https://angular.io/guide/quickstart)
- [Yarn](https://yarnpkg.com/lang/en/docs/install/) ... you can skip this one, but yarn is a lot faster than npm

#### Download the project with Git
The best option to download the project is to clone it initially with git ... 

    git clone https://github.com/isuretpolos/AetherOnePi.git
    
... then after each change made in our master repository you can download the newest version with 

    git pull

#### Install Angular 8 and other tools
Maybe you need an additional parameter for specifying **Angular 8** if you want to participate developing the web gui.

    npm install -g @angular/cli

Install Yarn (this command is for Windows. Check your OS installation command):
    
    choco install yarn
    
In your operating system add yarn bin folder to the environment and test if it is available:

    yarn --version

Then add a build angular kit ...

    yarn add @angular-devkit/build-angular --dev

Then change to the [gui folder](../gui) and execute **ng build**.
Finally **ng serve -o** for executing a mini server for the gui. The parameter -o opens the default browser.

    cd gui
    ng build
    ng serve -o

#### Missing ControlP5 Lib
Call **[installLibsDependencies.bat](../installLibsDependencies.bat)** for installing [ControlP5](http://www.sojamo.de/libraries/controlP5/) library in your local repository. Unfortunately this library is not available in [maven central repository](https://www.tutorialspoint.com/maven/maven_repositories.htm). You need to do this just once for your *developer environment*. I have added this call also in **mvnw.cmd**.
