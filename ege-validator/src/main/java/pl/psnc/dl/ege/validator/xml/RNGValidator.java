/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.psnc.dl.ege.validator.xml;

import com.thaiopensource.util.PropertyMap;
import com.thaiopensource.util.PropertyMapBuilder;
import com.thaiopensource.validate.IncorrectSchemaException;
import com.thaiopensource.validate.Schema;
import com.thaiopensource.validate.SchemaReader;
import com.thaiopensource.validate.ValidateProperty;
import com.thaiopensource.validate.Validator;
import com.thaiopensource.validate.auto.AutoSchemaReader;
import com.thaiopensource.validate.prop.rng.RngProperty;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.log4j.Logger;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 *
 * @author hac13
 */
public class RNGValidator implements XmlValidator {
    
    private static final Logger LOGGER = Logger.getLogger(RNGValidator.class);
    private final Schema schema;
    
    
    public RNGValidator(String url) throws 
            IOException, SAXException, IncorrectSchemaException {
       this(url, null, null);
    }
    
    public RNGValidator(String url, String defaultUrl) throws 
            IOException, SAXException, IncorrectSchemaException {
        this(url, defaultUrl, null);
    }
    
    public RNGValidator(String url, String defaultUrl, Map options) throws 
            IOException, SAXException, IncorrectSchemaException {
        PropertyMapBuilder pmb = new PropertyMapBuilder();
        if (options != null) {
            if ("false".equals(options.get("check_id_idref"))) {
                pmb.put(RngProperty.CHECK_ID_IDREF, null);
            }
        }
        if (url == null) {
            throw new IllegalArgumentException("Schema URL cannot be null.");
        }
        File f = new File(url);
        URL schemaURL;
        if (f.exists()) {
            schemaURL = f.toURI().toURL();
        } else {
            schemaURL = new URL(defaultUrl);
        }
        SchemaReader sr = new AutoSchemaReader();
        this.schema = sr.createSchema(new InputSource(schemaURL.openStream()), pmb.toPropertyMap());
    }
    
    public void validateXml(InputStream inputData, ErrorHandler errorHandler) 
            throws SAXException, IOException, Exception {
        Validator v = schema.createValidator(PropertyMap.EMPTY);
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setValidating(false);
        spf.setNamespaceAware(true);
        SAXParser parser = spf.newSAXParser();
        XMLReader reader = parser.getXMLReader();
        reader.setContentHandler(v.getContentHandler());
        reader.setErrorHandler(errorHandler);
        reader.parse(new InputSource(inputData));
    }

}
