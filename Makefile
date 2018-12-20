.PHONY: docker encode deploy

HOST = web01.trinitynashville.org
VERSION = $(shell git ver)
WAR = app/target/app.war

ring:
	cd app && lein ring server-headless

test:
	./Build.hs test-jetty

run:
	./Build.hs run-jetty

clean:
	./Build.hs clean

docker:
	./Build.hs docker-run

gcr:
	./Build.hs update-gcr

load-media:
	./Build.hs load-media

restart:
	ssh ubuntu@$(HOST) sudo svc -tu /etc/service/jetty

$(WAR):
	./Build.hs $(WAR)

upload: $(WAR)
	scp $(WAR) deploy@$(HOST):jetty/webapps/triweb.war

deploy: upload restart

converge:
	sudo bin/provision

$(HOME)/Downloads/$(DATE).mp3:
	wget -LO $(HOME)/Downloads/$(DATE).mp3 $(URL)

encode: $(HOME)/Downloads/$(DATE).mp3
#	ghc -O2 -o encode -ddump-minimal-imports bin/encode
	nix-shell --command "bin/encode.hs --setDate $(DATE) --imagePath app/resources/static/img/podcast5.png $(HOME)/Downloads/$(DATE).mp3 <app/search/source/$(DATE).json"
	./Build.hs app/project.clj

# Hack because Dropbox won't update anymore on the server
publish-podcast:
	rsync -avz ~/Dropbox/Trinity-WWW/www.trinitynashville.org/sermons trinity@web01.trinitynashville.org:Dropbox/Trinity-WWW/www.trinitynashville.org

aws-check:
	aws s3 ls s3://media.trinitynashville.org/$(DATE)
