# Set the base image to use
FROM eclipse-temurin:17-jre-alpine

RUN ln -s /usr/share/zoneinfo/Africa/Maputo /etc/localtime

# Set the working directory
WORKDIR /opt/app

# Copy the JAR file to the container
COPY api/target/api-*.jar /opt/app/hl7sync-api.jar

# Expose the port that your application is running on
EXPOSE 8081

# Start the application
CMD ["java", "-jar", "hl7sync-api.jar"]


