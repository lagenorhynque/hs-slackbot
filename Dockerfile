FROM naartjie/alpine-lein

RUN apk update && \
    apk --no-cache add curl ghc musl-dev nodejs-npm sudo zlib-dev

ADD . /opt/
WORKDIR /opt/

RUN curl -sSL https://get.haskellstack.org/ | sh && \
    stack config set system-ghc --global true && \
    stack ghci

RUN lein deps && lein cljsbuild once

CMD node target/server.js
