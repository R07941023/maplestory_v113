# Use a modern, supported OpenJDK 8 image
FROM eclipse-temurin:8-jdk

# Install necessary packages (xvfb for Xvfb)
# hadolint ignore=DL3008
RUN apt-get update && apt-get install -y \
    xvfb \
    libxtst6 \
    libxrender1 \
    libxi6 \
    libxext6 \
    libx11-6 \
    fontconfig \
    fonts-dejavu-core \
    && rm -rf /var/lib/apt/lists/*

# Set the working directory
WORKDIR /app

# Copy the local project files into the container
COPY . /app
RUN rm -f /app/settings.ini \
 && mv /app/settings_prod.ini /app/settings.ini

# Make the run script executable
RUN chmod +x run.sh

# Expose the necessary ports
EXPOSE 8484
EXPOSE 7575
EXPOSE 7576
EXPOSE 7577
EXPOSE 2555

# Start Xvfb and the application
CMD ["/bin/bash", "-c", "Xvfb :0 -screen 0 1024x768x16 & export DISPLAY=:0 && ./run.sh"]