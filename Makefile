clean:
	lein clean

package:
	git ver >etc/version.txt
	lein ring uberwar triweb.war

restart:
	ssh deploy@valve sudo svc -tu /service/jetty

upload: package
	scp target/triweb.war deploy@valve:/apps/jetty/webapps

deploy: upload restart
