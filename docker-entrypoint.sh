#!/bin/sh

# setting the webservice URL for the client
sed -i -e "s@http:\/\/localhost:8080\/ege-webservice\/@$WEBSERVICE_URL@" $JETTY_WEBAPPS/ege-webclient/WEB-INF/web.xml  

# Run the default jetty entrypoint script 
exec /docker-entrypoint.sh
