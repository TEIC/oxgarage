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

# install and rename saxon jar to avoid class loader issues with old versions
# see https://github.com/peterstadler/oxgarage-docker/issues/2#issuecomment-358663386
RUN unzip /tmp/saxon.zip -d ${OXGARAGE_BUILD_HOME}/saxon \ 
    && mv ${OXGARAGE_BUILD_HOME}/saxon/saxon9he.jar ${OXGARAGE_BUILD_HOME}/saxon/jsaxon9he.jar  

# build the application packages
RUN mvn install:install-file -DgroupId=jpf-tools -DartifactId=jpf-tools -Dversion=1.5.1 -Dpackaging=jar -Dfile=jpf-tools.jar \
    && mvn install:install-file -DgroupId=com.artofsolving -DartifactId=jodconverter -Dversion=3.0-beta-4 -Dpackaging=jar -Dfile=jod-lib/jodconverter-core-3.0-beta-4.jar \
    && mvn install:install-file -DgroupId=com.sun.star -DartifactId=jurt  -Dversion=3.2.1 -Dpackaging=jar -Dfile=jod-lib/jurt-3.2.1.jar \
    && mvn install:install-file -DgroupId=com.sun.star -DartifactId=juh   -Dversion=3.2.1 -Dpackaging=jar -Dfile=jod-lib/juh-3.2.1.jar \
    && mvn install:install-file -DgroupId=com.sun.star -DartifactId=unoil -Dversion=3.2.1 -Dpackaging=jar -Dfile=jod-lib/unoil-3.2.1.jar \
    && mvn install:install-file -DgroupId=com.sun.star -DartifactId=ridl  -Dversion=3.2.1 -Dpackaging=jar -Dfile=jod-lib/ridl-3.2.1.jar \
    && mvn install:install-file -DgroupId=org.apache.commons.cli -DartifactId=commons-cli -Dversion=1.1 -Dpackaging=jar -Dfile=jod-lib/commons-cli-1.1.jar \
    && mvn install:install-file -DgroupId=net.sf.saxon -DartifactId=commons-cli -Dversion=9.8 -Dpackaging=jar -Dfile=${OXGARAGE_BUILD_HOME}/saxon/jsaxon9he.jar \
    && mvn install

#########################################
# Now installing the Jetty server
# and adding our freshly built war packages
#########################################
FROM tomcat:7

ENV CATALINA_WEBAPPS ${CATALINA_HOME}/webapps
ENV OFFICE_HOME /usr/lib/libreoffice

USER root:root

RUN apt-get update \
    && apt-get install -y --force-yes libreoffice \
    ttf-dejavu \
    fonts-arphic-ukai \
    fonts-arphic-uming \
    ttf-baekmuk \
    ttf-junicode \
    fonts-linuxlibertine \
    fonts-ipafont-gothic \
    fonts-ipafont-mincho \
    && ln -s ${OFFICE_HOME} /usr/lib/openoffice \
    && rm -rf /var/lib/apt/lists/*

# copy some settings and entrypoint script
COPY ege-webservice/src/main/webapp/WEB-INF/lib/oxgarage.properties /etc/
COPY log4j.xml /var/cache/oxgarage/log4j.xml
COPY docker-entrypoint.sh /my-docker-entrypoint.sh

# copy build artifacts 
COPY --from=builder /opt/oxgarage-build/ege-webclient/target/ege-webclient.war /tmp/ege-webclient.war
COPY --from=builder /opt/oxgarage-build/ege-webservice/target/ege-webservice.war /tmp/ege-webservice.war
       
RUN rm -Rf ${CATALINA_WEBAPPS}/ROOT \
    && unzip -q /tmp/ege-webclient.war -d ${CATALINA_WEBAPPS}/ROOT/ \
    && unzip -q /tmp/ege-webservice.war -d ${CATALINA_WEBAPPS}/ege-webservice/ \
    && rm /tmp/ege-webclient.war \
    && rm /tmp/ege-webservice.war \
    && chmod 755 /my-docker-entrypoint.sh

# set rights for the jetty user who will run the services
#RUN chown -R jetty:jetty \
#    /var/cache/oxgarage \
#    ${CATALINA_WEBAPPS}/* \
#    /my-docker-entrypoint.sh \
#    && chmod 755 /my-docker-entrypoint.sh

VOLUME ["/usr/share/xml/tei/stylesheet", "/usr/share/xml/tei/odd"]

EXPOSE 8080 8081

ENTRYPOINT ["/my-docker-entrypoint.sh"]
CMD ["catalina.sh", "run"]
