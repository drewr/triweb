FROM voidlinux/voidlinux

RUN xbps-install -Suy \\
 && xbps-install -ASy openjdk-jre \\
 && xbps-remove -Oo

MAINTAINER $maintainer$

ENV USER deploy
ENV HOME /home/\$USER
ENV APPDIR /app
RUN groupadd \$USER && useradd -m -g \$USER \$USER
RUN mkdir \$APPDIR

COPY app.jar \$APPDIR
RUN chown -R \$USER \$APPDIR

COPY sermons.json.gz /

USER \$USER
CMD ["java", "-cp", "/app/app.jar", "triweb.boot"]
