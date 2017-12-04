.PHONY: docker encode deploy

HOST = web01.trinitynashville.org
VERSION = $(shell git ver)
WAR = app/target/app.war

test:
	./Build.hs test-jetty

run:
	./Build.hs run-jetty

clean:
	./Build.hs clean

docker:
	./Build.hs docker-run

restart:
	ssh ubuntu@$(HOST) sudo svc -tu /etc/service/jetty

$(WAR):
	./Build.hs $(WAR)

upload: $(WAR)
	scp $(WAR) deploy@$(HOST):jetty/webapps/triweb.war

deploy: upload restart

converge:
	sudo bin/provision

encode:
#	ghc -O2 -o encode -ddump-minimal-imports bin/encode
	bin/encode.hs --setDate $(DATE) --imagePath app/resources/static/img/podcast5.png ~/Downloads/$(DATE).mp3 <app/search/source/$(DATE).json
	cd app && lein run -m triweb.media.migration/print-legacy-post search/source/$(DATE).json

aws-check:
	aws s3 ls s3://media.trinitynashville.org/$(DATE)
