OxGarage
========


[![Build Status](https://travis-ci.com/TEIC/oxgarage.svg?branch=dev)](https://travis-ci.com/TEIC/oxgarage)
[![GitHub](https://img.shields.io/github/license/teic/oxgarage.svg)](https://github.com/TEIC/oxgarage/blob/dev/LICENSE)
[![GitHub release](https://img.shields.io/github/release/teic/oxgarage.svg)](https://github.com/TEIC/oxgarage/releases)
[![Docker Automated build](https://img.shields.io/docker/automated/teic/oxgarage.svg)](https://hub.docker.com/r/teic/oxgarage/)
[![Docker Build Status](https://img.shields.io/docker/build/teic/oxgarage.svg)](https://hub.docker.com/r/teic/oxgarage/)


OxGarage is a web, and RESTful, service to manage the transformation of documents between a variety of formats. The majority of transformations use Text Encoding Initiative XML as a pivot format, and Libre/Open Office to read some binary document formats

OxGarage is based on the Enrich Garage Engine (https://sourceforge.net/projects/enrich-ege/)
developed by Poznan Supercomputing and Networking Center and Oxford University Computing Services for the EU-funded ENRICH project

Installing
--------

### with Docker

If you have Docker installed, you can fetch a readymade image from [Docker Hub](https://hub.docker.com/r/teic/oxgarage/).
```bash
docker run --rm \
    -p 8080:8080 \
    -v /your/path/to/Stylesheets:/usr/share/xml/tei/stylesheet \ 
    -v /your/path/to/TEI/P5:/usr/share/xml/tei/odd \
    -e WEBSERVICE_URL=http://localhost:8080/ege-webservice/  \
    --name oxgarage teic/oxgarage
```
Once it's running, you can point your browser at `http://localhost:8080/` for the user interface.

#### available parameters

* **WEBSERVICE_URL** : The full URL of the RESTful *web service*. This is relevant for the *web client* (aka the GUI) if you are running the docker container on a different port or with a different URL.

NB: For running the image you'll need to have the TEI Stylesheets as well as the TEI P5 sources.
There are several ways to obtain these (see "Get and install a local copy" at http://www.tei-c.org/Guidelines/P5/),  
one of them is to download the latest release of both 
[TEI](https://github.com/TEIC/TEI/releases) and [Stylesheets](https://github.com/TEIC/Stylesheets/releases) from GitHub. 
Then, the Stylesheets' root directory (i.e. which holds the `profiles` directory) must be mapped to `/usr/share/xml/tei/stylesheet` whereas for the 
P5 sources you'll need to find the subdirectory which holds the file `p5subset.xml` and map this to `/usr/share/xml/tei/odd`; (should be `xml/tei/odd`).

#### exposed ports

The Docker image exposes two ports, 8080 and 8081. If you're running OxGarage over plain old HTTP, use the 8080 connector. 
For HTTPS connections behind a 
[SSL terminating Load Balancer](https://creechy.wordpress.com/2011/08/22/ssl-termination-load-balancers-java/), please use the 8081 connector.


###  without Docker

#### Getting the application packages

Application packages (WAR files) are available from the [OxGarage release page](https://github.com/TEIC/oxgarage/releases). These can also be extracted from the Docker image (e.g. when you want to test the dev version) by calling 

```bash
docker run --rm -v $(pwd)/tmp:/tmp teic/oxgarage sh -c "cp -r /usr/local/tomcat/webapps/ege-webservice /usr/local/tomcat/webapps/ROOT /tmp/"
```

which will copy the two folders `ROOT` and `ege-webservice` to a (newly created) subdirectory `tmp` in the current directory. If you further need to package these folders as WAR files run the two commands fom the current directory:

```bash
jar -cfv tmp/ege-webclient.war -C tmp/ROOT .  
jar -cfv tmp/ege-webservice.war -C tmp/ege-webservice .
```

NB: You will need to have Docker installed as well as a JavaJDK for the above commands to work. 

#### Running the application packages

If you have a running Tomcat (or similar container), you can install the WAR files (see above) in the normal way. In this case, you will need to do some configuration manually:

 1.   copy the file  `ege-webservice/WEB-INF/lib/oxgarage.properties` to `/etc/oxgarage.properties`
 2.   create a directory `/var/cache/oxgarage` and copy the file `log4j.xml` to there
 3.   make the directory owned by the Tomcat user, so that it can create files there: eg `chown -R tomcat6:tomcat6 /var/cache/oxgarage`
 4.   edit the file `webapps/ege-webclient/WEB-INF/web.xml` so that it has the hostname of the server set. eg
    ``perl -p -i -e "s/localhost/`hostname -f`/" /var/lib/tomcat6/webapps/ege-webclient/WEB-INF/web.xml``
 5.  make sure the TEI stylesheets and source are installed at `/usr/share/xml/tei` using the Debian file hierarchy standard; the distribution files at https://github.com/TEIC/TEI/releases and https://github.com/TEIC/Stylesheets/releases are in the right layout.

You'll probably need to restart your servlet container to make sure these changes take effect.

Edit the file `oxgarage.properties` if you need to change the names of directories.

Note also that OxGarage needs a headless OpenOffice available to do some of its work. The properties file specifies that this is
at `/usr/lib/openoffice/`, but this can be changed if needed

Check the working system by visiting /ege-webclient/ on your Tomcat (or similar) server, and trying an example transformation. You can check the RESTful web server using eg Curl. For example, to convert a TEI XML file to Word format, you might do

```bash
curl -s  -o test.docx -F upload=@test.xml http://localhost:8080/ege-webservice/Conversions/TEI%3Atext%3Axml/docx%3Aapplication%3Avnd.openxmlformats-officedocument.wordprocessingml.document
```

Building
------

OxGarage is written in Java and built using Maven, so the command

    mvn install

will normally do the job, if you have Maven, Java etc installed. The WAR files will be created in the `target` directory.

There are a variety of packages which may not be available in Maven repositories, so you may first need to install them locally, using these commands:

    mvn install:install-file -DgroupId=jpf-tools -DartifactId=jpf-tools -Dversion=1.5.1 -Dpackaging=jar -Dfile=jpf-tools.jar
    mvn install:install-file -DgroupId=com.artofsolving -DartifactId=jodconverter -Dversion=3.0-beta-4 -Dpackaging=jar -Dfile=jod-lib/jodconverter-core-3.0-beta-4.jar
    mvn install:install-file -DgroupId=com.sun.star -DartifactId=jurt  -Dversion=3.2.1 -Dpackaging=jar -Dfile=jod-lib/jurt-3.2.1.jar
    mvn install:install-file -DgroupId=com.sun.star -DartifactId=juh   -Dversion=3.2.1 -Dpackaging=jar -Dfile=jod-lib/juh-3.2.1.jar
    mvn install:install-file -DgroupId=com.sun.star -DartifactId=unoil -Dversion=3.2.1 -Dpackaging=jar -Dfile=jod-lib/unoil-3.2.1.jar
    mvn install:install-file -DgroupId=com.sun.star -DartifactId=ridl  -Dversion=3.2.1 -Dpackaging=jar -Dfile=jod-lib/ridl-3.2.1.jar
    mvn install:install-file -DgroupId=org.apache.commons.cli -DartifactId=commons-cli -Dversion=1.1 -Dpackaging=jar -Dfile=jod-lib/commons-cli-1.1.jar
    mvn install:install-file -DgroupId=net.sf.saxon -DartifactId=commons-cli -Dversion=9.8 -Dpackaging=jar -Dfile=jsaxon9he.jar


# How does the OxGarage work:

The program is divided into 8 parts: API, framework, 4 plug-ins (1 validator, 3 converters), web service and web client. API offers only the base, on which the framework is built. The role of the framework is to search for all provided plug-ins, initialize them and calculate all possible input types and conversion paths. To do this, it asks each converter to provide a list of all conversions it is able to do. Then the framework constructs a graph, where different document types are nodes and conversions are edges. This graph is directed and weighted. Weights to the edges are assigned based on a subjective judgement of how good or bad the resulting document looks. The better the document looks, the lower the weight. These weights are then summed together and only the path with minimal total weight is offered to the user in case there are several routes available from input format to output format. Framework also provides for processing the path of conversions that are needed to be done and performing the necessary conversions in a chain of threads, where one thread passes its result to the next thread until the desired output format is reached. Each thread does exactly one conversion and uses a converter to perform it.

The role of validator is to validate documents before conversions. This is done in order to stop user from transforming a malformed document, as this could cause an error during conversion, or an unexpected result. Unfortunately, the validator is only capable of validating very few document formats (some XML documents) and hence it is not used very often.

Then there are converters, which do the conversion from one format to another. Each converter must be able to provide a list of all possible conversions it can do and also perform a conversion. Currently there are 3 different converters: XslConverter, TEIConverter and OOConverter. XslConverter and TEIConverter are using xsl style-sheets to convert between different form of XML documents. The main difference between them is that TEIConverter is used for a more complex conversions, e.g. conversions to and from docx and odt. The OOConverter is using a JODConverter library to start OpenOffice.org in a headless mode and then calls it to convert a document. More plug-ins (both converters and validators) can be added quite easily. If you are interested in this, I suggest reading http://enrich-ege.sourceforge.net/creation.html.

Web service is a servlet, that uses the framework to perform conversions. It is REST-full and you can control it simply using POST and GET request. First you need to send GET request asking for all the possible input formats. Then you need to send another GET request to get all possible output formats from a given input format. After this, you need to POST your file into a given URL and that's it. This can be particularly useful for batch processing a large number of files. For more information read http://enrich-ege.sourceforge.net/restws.html. Of course, if you already know the URL for the conversion, it is enough to POST your file to this URL without having to go through all these steps.

The last important part of OxGarage is the web client. This is basically a user interface for the web service. The important thing about it is that it requires JavaScript to work. This web client simply sends GET and POST requests to web service and processes the responses.

# How to add new conversions

Adding new conversions can be done in two different ways. You can either build a new converter, or add new conversions into existing converters. Adding new conversions is rather different in each converter and you can find very brief instructions in the next sections. After you have added the format, you will also need to add new mime-type and extension pair into fileExt.xml file in the web service directory. It is strongly advised to use the same format description, format name and format mime-type for one document format, in case it is defined in several converters.

## Adding new conversions to XslConverter

This can be done very easily. All you have to do is to add the new style-sheets into you stylesheets directory. Then you need to provide a plugin.xml file specifying some properties of the conversion. For and example of such file, see profiles/default/csv folder in your stylesheets directory. After it is done, you only need to refresh the web client page and new conversion should appear. Note that you can also add new conversions by defining them in the ege-xsl-converter/src/main/resources/META-INF/plugin.xml file. But then you have to recompile the whole application.

## Adding new conversions to TEIConverter

This is a bit more difficult. First you need to add the conversion information into Format.java file. After this is done, you need to define the conversion in the TEIconverter.java file. You might also need to look into ConverterConfiguration.java in order to change some conversion settings. When everything is finished, you need to rebuild and redeploy the whole application.

## Adding new conversions to OOConverter

In order to do this, you need to add the document format into one of the files: InputTextFormat.java, InputSpreadsheetFormat.java, InputPresentationFormat.java, OutputTextFormat.java, OutputSpreadsheetFormat.java, OutputPresentationFormat.java. Then you need to change some of the Java files depending on the support of the new format by the JODConverter library.

# How to redefine weights of edges for conversions

As was mentioned before, each conversion is assigned a weight according to how much we trust the result. The better the result, the lower the weight. This has to be done, because there is a huge amount of possible ways how to get from input format to output format. Therefore, now the program chooses always the path with the smallest total weight, which is calculated as sum of weights of all conversions which form the path. If there is more than one path with the smallest total weight, one of the paths is chosen non-deterministically.

However, during time the conversions will surely become more refined and produce better results. Therefore, you might want to change the weights to make the service use the current best conversions more often. Again, what you need to do in order to change the weights depends quite a lot on the converter.

## Changing weights in XslConverter

To change the weights in XslConverter you need to change the value of “cost” parameter in plugin.xml file. This file can be found in ege-xsl-converter/src/main/resources/META-INF directory. If the conversion you are looking for is not there, it is possible that it was added by definition in stylesheets directory. In that case, you need to find the appropriate plugin.xml file in your stylesheets directory.

## Changing weights in TEIConverter

In this case you need to find Format.java file. There you can easily adjust the weights.

## Changing weights in OOConverter

In OOConverter weights are calculated as the sum of the input's and output's weight. Therefore, if for example in the new version of OpenOffice.org its ability to read docx files improves rapidly and you would like to reflect this in the weightings, you need to find the appropriate input type in the appropriate file. In this case it would be DOCX in InputTextFormat.java. Now you simply change the value of the cost variable and it's done.
