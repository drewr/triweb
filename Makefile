HOST = web01.trinitynashville.org

clean:
	lein clean

package:
	git ver >etc/version.txt
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
	PATH=~/tmp/py/bin:$$PATH bin/encode.hs --setDate $(DATE) ~/Downloads/$(DATE).mp3 <bin/$(DATE).yaml

aws-check:
	PATH=~/tmp/py/bin:$$PATH aws s3 ls s3://media.trinitynashville.org/$(DATE)
