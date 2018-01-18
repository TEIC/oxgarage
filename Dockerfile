#########################################
# multi stage Dockerfile for creating a Docker image
# 1. set up the build environment and build the war files
# 2. install a Jetty server with the web applications from 1
#########################################
FROM maven:3 as builder
LABEL maintainer="Peter Stadler for the TEI Council"

ENV OXGARAGE_BUILD_HOME="/opt/oxgarage-build"

ARG SAXON_URL="https://downloads.sourceforge.net/project/saxon/Saxon-HE/9.8/SaxonHE9-8-0-7J.zip" 

ADD ${SAXON_URL} /tmp/saxon.zip

WORKDIR ${OXGARAGE_BUILD_HOME}

COPY . .

RUN unzip /tmp/saxon.zip -d ${OXGARAGE_BUILD_HOME}/saxon \ 
    && mvn install:install-file -DgroupId=jpf-tools -DartifactId=jpf-tools -Dversion=1.5.1 -Dpackaging=jar -Dfile=jpf-tools.jar \
    && mvn install:install-file -DgroupId=com.artofsolving -DartifactId=jodconverter -Dversion=3.0-beta-4 -Dpackaging=jar -Dfile=jod-lib/jodconverter-core-3.0-beta-4.jar \
    && mvn install:install-file -DgroupId=com.sun.star -DartifactId=jurt  -Dversion=3.2.1 -Dpackaging=jar -Dfile=jod-lib/jurt-3.2.1.jar \
    && mvn install:install-file -DgroupId=com.sun.star -DartifactId=juh   -Dversion=3.2.1 -Dpackaging=jar -Dfile=jod-lib/juh-3.2.1.jar \
    && mvn install:install-file -DgroupId=com.sun.star -DartifactId=unoil -Dversion=3.2.1 -Dpackaging=jar -Dfile=jod-lib/unoil-3.2.1.jar \
    && mvn install:install-file -DgroupId=com.sun.star -DartifactId=ridl  -Dversion=3.2.1 -Dpackaging=jar -Dfile=jod-lib/ridl-3.2.1.jar \
    && mvn install:install-file -DgroupId=org.apache.commons.cli -DartifactId=commons-cli -Dversion=1.1 -Dpackaging=jar -Dfile=jod-lib/commons-cli-1.1.jar \
    && mvn install:install-file -DgroupId=net.sf.saxon -DartifactId=commons-cli -Dversion=9.8 -Dpackaging=jar -Dfile=${OXGARAGE_BUILD_HOME}/saxon/saxon9he.jar


#########################################
# Now installing the Jetty server
# and adding our freshly built war packages
#########################################
FROM jetty:alpine

ENV JETTY_WEBAPPS ${JETTY_BASE}/webapps
ENV OFFICE_HOME /usr/lib/libreoffice

USER root:root

RUN apk --update add libreoffice \
    ttf-dejavu \
    ttf-linux-libertine \ 
    font-noto \
    && ln -s ${OFFICE_HOME} /usr/lib/openoffice 

COPY --from=builder /opt/oxgarage-build/ege-webservice/target/ege-webservice/WEB-INF/lib/oxgarage.properties /etc/
COPY --from=builder /opt/oxgarage-build/log4j.xml /var/cache/oxgarage/log4j.xml
COPY --from=builder /opt/oxgarage-build/ege-webclient/target/ege-webclient.war /tmp/ege-webclient.war
COPY --from=builder /opt/oxgarage-build/ege-webservice/target/ege-webservice.war /tmp/ege-webservice.war
COPY --from=builder /opt/oxgarage-build/docker-entrypoint.sh /my-docker-entrypoint.sh
       
RUN mkdir ${JETTY_WEBAPPS}/ege-webclient \
    && mkdir ${JETTY_WEBAPPS}/ege-webservice \
    && unzip -q /tmp/ege-webclient.war -d ${JETTY_WEBAPPS}/ege-webclient/ \
    && unzip -q /tmp/ege-webservice.war -d ${JETTY_WEBAPPS}/ege-webservice/ \
    && rm /tmp/ege-webclient.war \
    && rm /tmp/ege-webservice.war

# add some Jetty jars needed for CORS support
ADD http://central.maven.org/maven2/org/eclipse/jetty/jetty-servlets/9.4.7.v20170914/jetty-servlets-9.4.7.v20170914.jar "$JETTY_WEBAPPS"/ege-webservice/WEB-INF/lib/
ADD http://central.maven.org/maven2/org/eclipse/jetty/jetty-util/9.4.7.v20170914/jetty-util-9.4.7.v20170914.jar "$JETTY_WEBAPPS"/ege-webservice/WEB-INF/lib/

# set rights for the jetty user who will run the services
RUN chown -R jetty:jetty \
    /var/cache/oxgarage \
    ${JETTY_WEBAPPS}/* \
    /my-docker-entrypoint.sh \
    && chmod 755 /my-docker-entrypoint.sh

USER jetty:jetty

VOLUME ["/usr/share/xml/tei/stylesheet", "/usr/share/xml/tei/odd"]

EXPOSE 8080

ENTRYPOINT ["/my-docker-entrypoint.sh"]
CMD ["java","-jar","/usr/local/jetty/start.jar"]
