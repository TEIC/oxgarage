#!/bin/sh

# setting the webservice URL for the client
sed -i -e "s@http:\/\/localhost:8080\/ege-webservice\/@$WEBSERVICE_URL@" ${CATALINA_WEBAPPS}/ege-webclient/WEB-INF/web.xml  

# run the command given in the Dockerfile at CMD 
exec "$@"