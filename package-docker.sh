#!/bin/sh
set -e

# Set variables
APP_NAME="amogus-irl"
DIST_DIR="src/main/client/dist"
DOCKER_DIR="dist/${APP_NAME}-docker"
JAR_NAME="backend-server.jar"
FINAL_JAR_PATH="target/${JAR_NAME}"

echo "Building Java backend..."
./mvnw clean package -DskipTests

echo "Building Vite frontend..."
cd src/main/client
npm install
npm run build
cd -

echo "Cleaning old Docker output..."
rm -rf "$DOCKER_DIR"
mkdir -p "$DOCKER_DIR/config"
mkdir -p "$DOCKER_DIR/avahi/services"
mkdir -p "$DOCKER_DIR/public"

echo "Copying files..."
cp "$FINAL_JAR_PATH" "$DOCKER_DIR/$JAR_NAME"
cp -r "$DIST_DIR"/* "$DOCKER_DIR/public"
cp src/main/resources/settings.toml "$DOCKER_DIR/config/settings.toml"

# Create Avahi configuration
cat > "$DOCKER_DIR/avahi/avahi-daemon.conf" <<EOF
[server]
host-name=amogus-irl
domain-name=local
use-ipv4=yes
use-ipv6=no

[publish]
publish-addresses=yes
publish-hinfo=yes
publish-workstation=yes
publish-domain=yes

[wide-area]
enable-wide-area=yes

[reflector]
enable-reflector=no
EOF

cat > "$DOCKER_DIR/avahi/services/amogus.service" <<EOF
<service-group>
  <name replace-wildcards="yes">amogus-irl</name>
  <service>
    <type>_http._tcp</type>
    <port>80</port>
  </service>
</service-group>
EOF

echo "Writing Dockerfile..."
cat > "$DOCKER_DIR/Dockerfile" <<EOF
FROM eclipse-temurin:22-jdk-alpine

WORKDIR /app

COPY $JAR_NAME ./
COPY config/ ./config/

EXPOSE 8080

CMD ["java", "-jar", "$JAR_NAME"]
EOF

echo "Writing docker-compose.yml..."
cat > "$DOCKER_DIR/docker-compose.yml" <<EOF
version: '3.8'

services:
  $APP_NAME:
    build:
      context: .
    container_name: $APP_NAME
    ports:
      - "8080:8080"
    volumes:
      - config:/app/config

  ${APP_NAME}-web:
    image: httpd:2.4-alpine
    container_name: ${APP_NAME}-web
    ports:
      - "80:80"
    volumes:
      - ./public:/usr/local/apache2/htdocs:ro

  ${APP_NAME}-mdns:
    image: debian:bullseye
    container_name: ${APP_NAME}-mdns
    network_mode: "host"
    volumes:
      - ./avahi/avahi-daemon.conf:/etc/avahi/avahi-daemon.conf:ro
      - ./avahi/services:/etc/avahi/services:ro
    command: sh -c "apt-get update && apt-get install -y avahi-daemon && avahi-daemon -f"

volumes:
  config:
EOF

echo "Packaging into tar.gz..."
tar -czf "dist/${APP_NAME}-docker.tar.gz" -C "dist" "${APP_NAME}-docker"

echo "Done. Output is at: dist/${APP_NAME}-docker.tar.gz"
