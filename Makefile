.PHONY: up down test seed clean build

# Start all containers
up:
	docker-compose up -d

# Stop all containers
down:
	docker-compose down

# Run all tests
test:
	./mvnw clean verify

# Run only unit tests
unit-test:
	./mvnw clean test

# Clean and build
build:
	./mvnw clean package -DskipTests

# Clean Docker volumes
clean:
	docker-compose down -v

# Seed sample data
seed:
	./mvnw spring-boot:run -Dspring.profiles.active=local -Dapp.seed=true

# Start application in local mode
run:
	./mvnw spring-boot:run -Dspring.profiles.active=local

# Start application in clustered mode
run-clustered:
	./mvnw spring-boot:run -Dspring.profiles.active=local -Dquartz.clustered=true
