FROM voidlinux/voidlinux
MAINTAINER $maintainer$

RUN xbps-install -A -y -S openjdk \
 && xbps-install -A -y -S curl \
 && xbps-install -A -y -S rsync \
 && rm -rf /var/cache/xbps/*

COPY /elasticsearch /etc/services.d/elasticsearch/

## Sigh, /bin is a symlink to ./usr/bin so we have to repair it after
## extracting
RUN curl -LO https://github.com/just-containers/s6-overlay/releases/download/v1.20.0.0/s6-overlay-amd64.tar.gz \
 && tar zxf s6-overlay-amd64.tar.gz -C / \
 && rm /usr/bin/execlineb \
 && cp /bin/* /usr/bin \
 && rm /bin/* \
 && rmdir /bin \
 && ln -snf usr/bin /bin

RUN curl -LO https://artifacts.elastic.co/downloads/elasticsearch/elasticsearch-$esVersion$.tar.gz \
 && tar zxf elasticsearch-$esVersion$.tar.gz \
 && rm elasticsearch-$esVersion$.tar.gz \
 && mv elasticsearch-$esVersion$ /elasticsearch \
 && chown -R nobody /elasticsearch

ENTRYPOINT [ "/init" ]

