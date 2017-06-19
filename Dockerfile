FROM naartjie/alpine-lein

RUN apk update && \
    apk --no-cache add curl ghc musl-dev nodejs-npm sudo zlib-dev

ADD . /opt/
WORKDIR /opt/

RUN curl -sSL https://get.haskellstack.org/ | sh && \
    stack ghci --system-ghc

RUN lein deps && lein cljsbuild once

CMD node --optimize_for_size --max_old_space_size=460 --gc_interval=100 target/server.js
