package pl.psnc.dl.ege.validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import pl.psnc.dl.ege.types.DataType;
import pl.psnc.dl.ege.validator.xml.RNGValidator;
import pl.psnc.dl.ege.validator.xml.SchemaValidator;
import pl.psnc.dl.ege.validator.xml.XmlValidator;

/*
Requirements:
For validator, read (by default) http://www.tei-c.org/release/xml/tei/custom/schema/ and list available schemas
 - On initialization, read the rng and xsd folders, grab schemas prefixed with "tei_", cache them in a local folder
 - If equivalent copy exists, bon't update

Input customization name and POSTed file to be validated. No uploading schema files
Schematron validation
question whether do Relax NG / XSD valiation with Schematron in a single pass or separately
Roma needs fragment validation

RomaJS could be bundled as a frontend for OxGarage in the OxG Docker distribution

HC will also look at rate-limiting for OxG, since apparently it is abused for batch conversion. 

*/

/**
 * Singleton - prepares available XML validators.<br/>
 * Basic configuration of every XML validator is parsed from 'validators.xml'
 * file - provided with this implementation.
 * 
 * @author mariuszs
 */
public class XmlValidatorsProvider extends DefaultHandler {


	private static final Logger logger = Logger
			.getLogger(XmlValidatorsProvider.class);

	/*
	 * One XML validator for data type.
	 */
	private final Map<DataType, XmlValidator> xmlValidators = new HashMap<DataType, XmlValidator>();

	/**
	 * XML configuration : validators element
	 */
	public static final String T_VALIDATORS = "validators";

	/**
	 * XML configuration : validator element
	 */
	public static final String T_VALIDATOR = "validator";

	/**
	 * XML configuration : format attribute
	 */
	public static final String A_FORMAT = "format";

	/**
	 * XML configuration : name attribute
	 */
	public static final String A_NAME = "name";

	/**
	 * XML configuration : scheme attribute
	 */
	public static final String T_SCHEMA = "schema";

	/**
	 * XML configuration : Relax NG attribute
	 */
	public static final String T_RNG = "rng";

	/**
	 * XML configuration : url attribute
	 */
	public static final String A_URL = "url";
        
        public static final String A_DEFAULT = "defaultUrl";

	/**
	 * XML configuration : root attribute
	 */
	public static final String A_ROOT = "root";
        
        private String validator;



	/**
	 * Informs provider that it has to use default EAD DTD reference (contained
	 * in .jar file most likely)
	 */

	private XmlValidatorsProvider() {
		try {
                    /*
                        Instead, read in directory listing from http://www.tei-c.org/release/xml/tei/custom/schema/relaxng/ 
                        and–oops— can't do that because even weird stuff has a tei_ prefix. We'll have to have a list
                    */
			XMLReader xmlReader = XMLReaderFactory.createXMLReader();
			xmlReader.setContentHandler(this);
			xmlReader.parse(new InputSource(this.getClass()
					.getResourceAsStream("/validators.xml")));
                        
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
		}
	}

	/*
	 * Thread safe singleton.
	 */
	private static class XmlValidatorsProviderHolder {
		private static final XmlValidatorsProvider INSTANCE = new XmlValidatorsProvider();
	}

	/**
	 * Provider as singleton - method returns instance.
	 * 
	 * @return
	 */
	public static XmlValidatorsProvider getInstance() {
		return XmlValidatorsProviderHolder.INSTANCE;
	}

	/* TODO : properly handled errors - empty xml attributes etc. */
	@Override
	public void startElement(String uri, String localName, String name,
			Attributes attributes) throws SAXException {
		try {
			if (localName.equals(T_VALIDATOR)) {
				validator = attributes.getValue(A_NAME);
			} else {
				if (validator == null && localName.equals(T_VALIDATOR)) {
					throw new SAXException("Validators must have a @name in validation.xml");
				}
				if (localName.equals(T_RNG)) {
                                        Map<String,String> options = new HashMap<String,String>();
                                        options.put("check_id_idref", attributes.getValue("check_id_idref"));
					XmlValidator val = new RNGValidator(
                                                attributes.getValue(A_URL),
                                                attributes.getValue(A_DEFAULT),
                                                options);
					xmlValidators.put(new DataType(validator + "-RNG", "text/xml"), val);
				} else if (localName.equals(T_SCHEMA)) {
					String schemaUrl = attributes.getValue(A_URL);
					String defaultUrl = null;
					XmlValidator val = new SchemaValidator(schemaUrl,
							defaultUrl);
					xmlValidators.put(new DataType(validator+ "-XSD", "text/xml"), val);
				}
			}
		} catch (Exception ex) {
			throw new SAXException("Configuration errors occured.");
		}
	}

	/**
	 * Returns XML validator by selected data type (null if data type is not
	 * supported).
	 * 
	 * @param dataType
	 * @return
	 */
	public XmlValidator getValidator(DataType dataType) {
		return xmlValidators.get(dataType);
	}

	/**
	 * Returns supported data types.
	 * 
	 * @return
	 */
	public List<DataType> getSupportedDataTypes() {
		return new ArrayList<DataType>(xmlValidators.keySet());
	}

}
