FROM eclipse-temurin:21-jdk-jammy

# Install utilities
RUN apt-get update && apt-get install -y curl jq git dos2unix && rm -rf /var/lib/apt/lists/*

# Copy the entrypoint script
COPY entrypoint.sh /entrypoint.sh
RUN dos2unix /entrypoint.sh && chmod +x /entrypoint.sh

# The engine JAR would typically be downloaded from Maven Central or releases, 
# but for the action we assume it's packaged or we fetch it in the entrypoint.

ENTRYPOINT ["/entrypoint.sh"]
