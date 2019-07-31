# Documentation
## Quick Guide
The current version of AetherOnePi is the V1.0, which represents the "Standalone Version" with only a client capable of analysing and broadcasting. The analysis is performed with the virtual "stick pad".

### Installation
Download the version 1.0 [here](https://github.com/isuretpolos/AetherOnePi/releases/tag/1.0) and unzip the file where you want.
You need [Java Runtime 8](https://java.com/en/download/help/download_options.xml) or bigger. You can check if you have Java already installed on your operating system by typing

    java -version

### Start the Standalone Client
 ![Dashboard](https://raw.githubusercontent.com/isuretpolos/AetherOnePi/master/documentation/screenshots/startGuiBat.jpg)
 
 On Windows doubleclick on "startGui.bat". Or on other systems like MacOS or Linux you can open a terminal and type
 
     java -jar AetherOnePi-1.0.0-jar-with-dependencies.jar
     
 The jar is an executable only on OS which has this feature enabled.

### Dashboard
When you start the Client, the first screen is the Dashboard. Here you see new informations regarding Updates.

![Dashboard](https://raw.githubusercontent.com/isuretpolos/AetherOnePi/master/documentation/screenshots/dashboard.jpg)

In the Session screen you can insert a new target or load an existing one.

![Dashboard](https://raw.githubusercontent.com/isuretpolos/AetherOnePi/master/documentation/screenshots/session.jpg)

Next screen is the Analysis screen. For the Standalone version you need here only two buttons, the "SELECT DATA" and the "STICKPAD".

The "SELECT DATA" opens a dialog where you can select a rate list.

When you are ready to analyze, click on the "STICKPAD" button and move your mouse around until the analysis is complete. This requires some time.

Continue to move your mouse when the result appears, because this is required to check the General Vitality (GV).

What you then see is a list of rates ordered by the EV (energetic value).

Click on BROADCAST button for encoding and broadcasting the desired signature.

Or click on the GOOGLE button for researching the keyword.

If a rate has a URL you can open it with the URL button.

![Dashboard](https://raw.githubusercontent.com/isuretpolos/AetherOnePi/master/documentation/screenshots/analysis.jpg)

A rate can be broadcast also directly in the Broadcast screen. 

![Dashboard](https://raw.githubusercontent.com/isuretpolos/AetherOnePi/master/documentation/screenshots/broadcast.jpg)

# Developer Guide
Note: I will not give support to the novice in java programming.
I assume that you are a professional developer if you read this chapter.
I rather concentrate my time in research, developing and writing about radionics.
It is not my task to teach you programming (even if I would love to ... believe me!).

## Requirements
The following requirements are also the skills you need to master for partecipating to the project. Note that these are well known industrial standards.
But if you want to just build your own release, basic knowledge should be sufficient.

- [Git](https://git-scm.com/downloads)
- [Java 8](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
- [Maven 3](https://maven.apache.org/download.cgi)
- [NodeJS](https://nodejs.org/en/download/)
- [Chocolatey](https://chocolatey.org/) ... for Windows only (other OS use their own package manager)
- [Angular CLI](https://angular.io/guide/quickstart)
- [Yarn](https://yarnpkg.com/lang/en/docs/install/) ... you can skip this one, but yarn is a lot faster than npm

### Download the project with Git
The best option to download the project is to clone it initially with git ... 

    git clone https://github.com/isuretpolos/AetherOnePi.git
    
... then after each change made in our master repository you can download the newest version with 

    git pull

### Install Angular 8 and other tools
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

### Missing ControlP5 Lib
Call **[installLibsDependencies.bat](../installLibsDependencies.bat)** for installing [ControlP5](http://www.sojamo.de/libraries/controlP5/) library in your local repository. Unfortunately this library is not available in [maven central repository](https://www.tutorialspoint.com/maven/maven_repositories.htm). You need to do this just once for your *developer environment*. I have added this call also in **mvnw.cmd**.

    installLibsDependencies.bat
    
or 

    mvnw

### Build the project
    mvn clean install

Inside the **target folder** two different jar files are created. One is for the standalone gui and the other for the Raspberry Pi server application.
