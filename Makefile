HOST = web01.trinitynashville.org

bootstrap: etc/version.txt

etc/version.txt:
	mkdir -p etc
	git ver >etc/version.txt

clean:
	lein clean

package: bootstrap
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

aws-check:
	PATH=~/tmp/py/bin:$$PATH aws s3 ls s3://media.trinitynashville.org/$(DATE)
