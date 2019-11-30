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

1. Provision a VPS (DigitalOcean Droplet, AWS EC2, etc...) & SSH into it.
2.

# Local Dev

1. `./gradlew run`

The service will be reachable at `http://localhost:XXXX`.
