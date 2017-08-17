.PHONY: package version

HOST = web01.trinitynashville.org
VERSION = $(shell git ver)

version:
	mkdir -p etc
	echo $(VERSION) >etc/version.txt

pkg/Dockerfile: *.m4
	mkdir -p pkg
	cd pkg && m4 -I .. ../Dockerfile.m4 >Dockerfile

package: pkg/Dockerfile
	rsync -avz elasticsearch pkg
	docker build -t trinitynashville/media:$(VERSION) pkg

clean:
	lein clean

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
	PATH=~/tmp/py/bin:$$PATH bin/encode.hs --setDate $(DATE) ~/Downloads/$(DATE).mp3 <search/source/$(DATE).json
	lein run -m triweb.media.migration/print-legacy-post search/source/$(DATE).json

aws-check:
	PATH=~/tmp/py/bin:$$PATH aws s3 ls s3://media.trinitynashville.org/$(DATE)
