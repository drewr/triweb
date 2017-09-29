.PHONY: package version docker encode

HOST = web01.trinitynashville.org
VERSION = $(shell git ver)

test:
	./Build.hs test-jetty

clean:
	./Build.hs clean

docker:
	./Build.hs docker-run

triweb.war: etc/version.txt
	lein ring uberwar triweb.war

restart:
	ssh ubuntu@$(HOST) sudo svc -tu /etc/service/jetty

upload: package
	scp target/triweb.war deploy@$(HOST):jetty/webapps

deploy: upload restart

converge:
	sudo bin/provision

encode:
#	ghc -O2 -o encode -ddump-minimal-imports bin/encode
	PATH=~/tmp/py/bin:$$PATH bin/encode.hs --setDate $(DATE) --imagePath app/resources/static/img/podcast5.png ~/Downloads/$(DATE).mp3 <app/search/source/$(DATE).json
	cd app && lein run -m triweb.media.migration/print-legacy-post search/source/$(DATE).json

aws-check:
	PATH=~/tmp/py/bin:$$PATH aws s3 ls s3://media.trinitynashville.org/$(DATE)
