# Estimation Service

Mobile App: https://github.com/talham7391/estimation-godot

# Setup

I developed an [estimation library](https://github.com/talham7391/estimation) which this service uses to facilitate gameplay. Ideally this library should be published and included as a dependency, but as a quick work around for this project use the following steps to include it in the project:

1. `git clone git@github.com:talham7391/estimation-service.git`
2. `cd estimation-service`
3. `mkdir external`
4. `cd external`
5. `git clone git@github.com:talham7391/estimation.git`
6. `cd ..`
7. Run `./gradlew build` to make sure everything is working.

# Deployment

### Setting up the VPS

1. Provision a VPS (DigitalOcean Droplet, AWS EC2, etc...).
1. Build a JAR and copy it to the VPS:
	1. `./gradlew shadowJar`
	2. `scp build/libs/estimationserver-all.jar <user>@<server_address>:<location>`
	3. `ssh <user>@<server_address>:<location>`
2. Run the following commands:
    1. `sudo apt update -y`
    2. `sudo apt install openjdk-8-jre -y`
    3. Run `java -version` to verify java is installed.
    4. `cd` to where you copied the JAR.
    4. `java -jar estimationserver-all.jar`

At this point, you should be able to reach the service at whatever hostname routes to your VPS. *Be mindful of which port the service is listening on.*

# Local Dev

1. `./gradlew run`

The service will be reachable at `http://localhost:XXXX`.
