#!/bin/sh
set -e

# Set variables
APP_NAME="amogus-irl"
CLIENT_DIR="src/main/client"
DOCKER_DIR="dist/${APP_NAME}-docker"
JAR_NAME="backend-server-jar-with-dependencies.jar"
FINAL_JAR_PATH="target/${JAR_NAME}"

echo "Building Java backend..."
./mvnw clean package -DskipTests

echo "Building Vite frontend..."
cd "$CLIENT_DIR"
npm install
npm run build
cd -

echo "Cleaning old Docker output..."
rm -rf "$DOCKER_DIR"
mkdir -p "$DOCKER_DIR/config"
mkdir -p "$DOCKER_DIR/public"

echo "Copying files..."
cp "$FINAL_JAR_PATH" "$DOCKER_DIR/$JAR_NAME"
cp -r "$CLIENT_DIR"/dist/* "$DOCKER_DIR/public"
cp -r "$CLIENT_DIR"/public/* "$DOCKER_DIR/public"
cp src/main/resources/settings.toml "$DOCKER_DIR/config/settings.toml"

echo "Writing Dockerfile for backend..."
cat > "$DOCKER_DIR/Dockerfile.backend" <<EOF
FROM eclipse-temurin:22-jdk-alpine

WORKDIR /app

COPY $JAR_NAME ./
COPY config/ ./config/

EXPOSE 8080

CMD ["java", "-jar", "$JAR_NAME"]
EOF

echo "Writing Dockerfile for mDNS sidecar..."
cat > "$DOCKER_DIR/Dockerfile.mdns" <<EOF
FROM python:3.12-slim

RUN pip install zeroconf

COPY mdns.py /app/mdns.py

CMD ["python", "/app/mdns.py"]
EOF

echo "Writing mDNS Python script..."
cat > "$DOCKER_DIR/mdns.py" <<EOF
from zeroconf import ServiceInfo, Zeroconf
import socket
import time
import logging

logging.basicConfig(level=logging.INFO, format='%(asctime)s %(message)s')

s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
try:
    s.connect(("8.8.8.8", 80))  # Doesn't send data, just sets up routing
    ip = s.getsockname()[0]
finally:
    s.close()

logging.info(f"Using IP address: {ip}")

info1 = ServiceInfo(
    "_http._tcp.local.",
    "amogus-80._http._tcp.local.",
    addresses=[socket.inet_aton(ip)],
    port=80,
    properties={},
    server="amogus.local."
)

info2 = ServiceInfo(
    "_http._tcp.local.",
    "amogus-8080._http._tcp.local.",
    addresses=[socket.inet_aton(ip)],
    port=8080,
    properties={},
    server="amogus.local."
)

zeroconf = Zeroconf()
zeroconf.register_service(info1)
logging.info("Registered service amogus-80 on port 80")
zeroconf.register_service(info2)
logging.info("Registered service amogus-8080 on port 8080")

try:
    while True:
        logging.info("Broadcasting amogus.local on ports 80 and 8080")
        time.sleep(60)
except KeyboardInterrupt:
    logging.info("Interrupted. Unregistering services...")
finally:
    zeroconf.unregister_all_services()
    zeroconf.close()
    logging.info("Services unregistered and Zeroconf closed.")
EOF

echo "Writing docker-compose.yml..."
cat > "$DOCKER_DIR/docker-compose.yml" <<EOF
version: '3.8'

services:
  $APP_NAME:
    build:
      context: .
      dockerfile: Dockerfile.backend
    container_name: $APP_NAME
    ports:
      - "8080:8080"
    restart: unless-stopped
    volumes:
      - ./config:/app/config

  ${APP_NAME}-web:
    image: httpd:2.4-alpine
    container_name: ${APP_NAME}-web
    ports:
      - "80:80"
    restart: unless-stopped
    volumes:
      - ./public:/usr/local/apache2/htdocs:ro

  ${APP_NAME}-mdns:
    build:
      context: .
      dockerfile: Dockerfile.mdns
    container_name: ${APP_NAME}-mdns
    network_mode: host
    restart: unless-stopped
EOF

echo "Packaging into tar.gz..."
tar -czf "dist/${APP_NAME}-docker.tar.gz" -C "dist" "${APP_NAME}-docker"

echo "Done. Output is at: dist/${APP_NAME}-docker.tar.gz"
