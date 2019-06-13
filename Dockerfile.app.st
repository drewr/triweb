FROM archlinux/base

RUN pacman -Sy \\
 && pacman -S --noconfirm curl jdk-openjdk \\
 && pacman -Scc --noconfirm

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
