REMOTE = deploy@web01.trinitynashville.org

clean:
	lein clean

package:
	git ver >etc/version.txt
	lein ring uberwar triweb.war

restart:
	ssh $(REMOTE) sudo svc -tu /service/jetty

upload: package
	scp target/triweb.war $(REMOTE):jetty/webapps

deploy: upload restart

converge:
	sudo bin/provision

encode:
	ghc -O2 -o encode -ddump-minimal-imports bin/encode
