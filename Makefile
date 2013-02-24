clean:
	lein clean

package:
	git ver >etc/version.txt
	lein ring uberwar triweb.war

deploy: package
	scp target/triweb.war deploy@valve:/apps/jetty/webapps
	ssh deploy@valve sudo svc -tu /service/jetty
