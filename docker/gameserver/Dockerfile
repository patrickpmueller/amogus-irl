FROM eclipse-temurin:25-alpine
WORKDIR /server

RUN mkdir ./config
COPY settings.toml ./config/
COPY gameserver-full.jar ./
EXPOSE 8080

RUN addgroup -S appgroup && adduser -S appuser -G appgroup -D
RUN chown -R appuser:appgroup /server
USER appuser

CMD ["java", "-jar", "gameserver-full.jar"]
