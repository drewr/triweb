FROM voidlinux/voidlinux
MAINTAINER $maintainer$

RUN xbps-install -A -y -S openjdk curl \\
 && rm -rf /var/cache/xbps/*

ENV USER deploy
ENV HOME /home/\$USER
ENV APPDIR /app
RUN groupadd \$USER && useradd -m -g \$USER \$USER
RUN mkdir \$APPDIR

COPY app.jar \$APPDIR
RUN chown -R \$USER \$APPDIR

EXPOSE 9000
USER \$USER
CMD ["java", "-cp", "/app/app.jar", "triweb.boot"]
