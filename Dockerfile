# Use a Flink base image
FROM apache/flink:1.16.0-scala_2.12

# Set working directory
WORKDIR /opt/flink/usrlib

# Copy the built jar from the host to the container
COPY build/libs/flink-job-1.0.jar /opt/flink/usrlib/

# Set the entry point to the job jar
ENTRYPOINT ["flink", "run", "-d", "-c", "flink.MainKt", "/opt/flink/usrlib/flink-job-1.0.jar"]