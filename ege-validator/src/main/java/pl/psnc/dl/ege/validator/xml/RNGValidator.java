/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package pl.psnc.dl.ege.validator.xml;

import com.thaiopensource.util.PropertyMapBuilder;
import com.thaiopensource.validate.SchemaReader;
import com.thaiopensource.validate.ValidateProperty;
import com.thaiopensource.validate.ValidationDriver;
import com.thaiopensource.validate.auto.AutoSchemaReader;
import com.thaiopensource.validate.prop.rng.RngProperty;
import com.thaiopensource.xml.sax.ErrorHandlerImpl;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.Map;
import org.apache.log4j.Logger;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author hac13
 */
public class RNGValidator implements XmlValidator {
    
    private static final Logger LOGGER = Logger.getLogger(SchemaValidator.class);
    private final String schemaUrl;
    private String defaultUrl = null;
    private PropertyMapBuilder options = new PropertyMapBuilder();
    
    
    public RNGValidator(String url) {
        if (url == null) {
            throw new IllegalArgumentException("Schema URL cannot be null.");
        }
        this.schemaUrl = url;
    }
    
    public RNGValidator(String url, String defaultUrl) {
        this(url);
        this.defaultUrl = defaultUrl;
    }
    
    public RNGValidator(String url, String defaultUrl, Map options) {
        this(url, defaultUrl);
        if ("false".equals(options.get("check_id_idref"))) {
            this.options.put(RngProperty.CHECK_ID_IDREF, null);
        }
    }
    
    public void validateXml(InputStream inputData, ErrorHandler errorHandler) throws SAXException, IOException, Exception {
        SchemaReader sr = new AutoSchemaReader();
        PropertyMapBuilder localOptions = new PropertyMapBuilder();
        localOptions.add(this.options.toPropertyMap());
        localOptions.put(ValidateProperty.ERROR_HANDLER, errorHandler);
        ValidationDriver driver = new ValidationDriver(localOptions.toPropertyMap(), sr);
        File localSchema = new File(schemaUrl);
        URL schemaURL = null;
        if (localSchema.exists()) {
            schemaURL = localSchema.toURI().toURL();
        } else {
            schemaURL = new URL(defaultUrl);
        }
        driver.loadSchema(new InputSource(schemaURL.openStream()));
        driver.validate(new InputSource(inputData));
    }

}
