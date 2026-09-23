# Use the official Python slim image as the base
FROM python:3.10-slim

# Install OpenJDK 17 (Java) required for compiling and running the simulator
RUN apt-get update && \
    apt-get install -y default-jdk && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/*

# Set the working directory
WORKDIR /app

# Copy the requirements file and install python dependencies
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copy the rest of the application
COPY . .

# Expose the dynamic port used by Render
EXPOSE $PORT

# Run the server
CMD ["python", "server.py"]
