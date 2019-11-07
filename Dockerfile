FROM alpine:3.10 AS builder

RUN apk add --no-cache --update  \
        bison~=3.3.2 \
        bsd-compat-headers~=0.7 \
        flex~=2.6 \
        g++~=8.3 \
        gcc~=8.3 \
        git~=2.22 \
        libnl3-dev~=3.4 \
        linux-headers~=4.19 \
        make~=4.2 \
        protobuf-dev~=3.6

RUN git clone https://github.com/google/nsjail.git /nsjail \
    && cd /nsjail \
    && git checkout 1111bb135a8a13231c8754cf0b45b58e4c0e9cb6
    # && git checkout 0b1d5ac03932c140f08536ed72b4b58741e7d3cf

WORKDIR /nsjail
RUN make

FROM zenika/kotlin:1.3.50-jdk12-alpine

COPY --from=builder /nsjail/nsjail /usr/sbin
RUN chmod +x /usr/sbin/nsjail

RUN apk add --no-cache --update \
        libnl3~=3.4 \
        libstdc++~=8.3 \
        protobuf~=3.6

# TODO Use "http://dl-cdn.alpinelinux.org/alpine/edge/main" >> /etc/apk/repositories" instead and merge apk calls.
RUN sed -i -e 's/v[[:digit:]]\..*\//edge\//g' /etc/apk/repositories
RUN apk add python3=3.8.0-r0 && apk upgrade musl
RUN mkdir /app

COPY ./build/libs/dune-latest.jar /app/dune-latest.jar
WORKDIR /app

EXPOSE 8080

CMD ["java", "-server", "-XX:+UnlockExperimentalVMOptions", \
     "-jar", "dune-latest.jar"]
