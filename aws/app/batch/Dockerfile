FROM eclipse-temurin:25.0.3_9-jre
WORKDIR /app
COPY ./batch.jar /app
ENV JAVA_OPTS="-Xmx1g -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp/heapdump.hprof"
CMD ["sh", "-c", "exec java $JAVA_OPTS -jar batch.jar"]
