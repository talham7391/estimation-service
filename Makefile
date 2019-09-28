DOCKER_ACC=talham7391
APP_NAME=estimationserver

IMAGE=${DOCKER_ACC}/${APP_NAME}:latest
STACK_NAME=${APP_NAME}-stack


.PHONY: buildimages
buildimages:

	./gradlew shadowJar
	docker build . -t ${IMAGE}


.PHONY: pushimages
pushimages:

	docker push ${IMAGE}


.PHONY: start
start: buildimages

	docker run --rm -p 80:80 --name ${APP_NAME} ${IMAGE}


.PHONY: stop
stop:

	docker stop ${APP_NAME}


.PHONY: deploy
deploy: buildimages pushimages

	aws cloudformation deploy \
	--template-file templates/infrastructure.yaml \
	--stack-name ${STACK_NAME} \
	--capabilities CAPABILITY_IAM \
	--parameter-overrides \
		ClusterName="${APP_NAME}-cluster" \
		AppImage="${IMAGE}"


.PHONY: serveraddy
serveraddy:

	$(eval AUTOSCALE_ARN := $(shell \
	aws cloudformation describe-stack-resources \
	--stack-name ${STACK_NAME} \
	--query '(StackResources[?ResourceType==`AWS::AutoScaling::AutoScalingGroup`].PhysicalResourceId)[0]' \
	| tr -d \"))

	$(eval INSTANCE_ID := $(shell \
	aws autoscaling describe-auto-scaling-groups \
	--auto-scaling-group-names ${AUTOSCALE_ARN} \
	--query 'AutoScalingGroups[0].Instances[0].InstanceId' \
	| tr -d \"))

	@echo $(shell aws ec2 describe-instances \
	--instance-ids ${INSTANCE_ID} \
	--query 'Reservations[0].Instances[0].PublicIpAddress' \
	| tr -d \")


.PHONY: teardown
teardown:

	aws cloudformation delete-stack \
	--stack-name ${STACK_NAME}


.PHONY: eject
eject:

	@if [ -z "$(project-name)" ]; then \
		echo "Usage: make eject project-name=\"example\""; \
		exit 1; \
	fi

	mv src/main/kotlin/example "src/main/kotlin/$(project-name)"
	mv src/test/kotlin/example "src/test/kotlin/$(project-name)"

	find src -type f -print0 | xargs -0 sed -i "" -e "s/example/$(project-name)/g"
	find templates -type f -print0 | xargs -0 sed -i "" -e "s/example/$(project-name)/g"

	sed -i "" -e "s/example/$(project-name)/g" settings.gradle
	sed -i "" -e "s/example/$(project-name)/g" "Dockerfile"
