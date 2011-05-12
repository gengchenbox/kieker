package kieker.analysis.util;

import java.util.HashMap;
import java.util.StringTokenizer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author Andre van Hoorn
 */
public class PropertyMap {
    private static final Log log = LogFactory.getLog(PropertyMap.class);

    private final HashMap<String, String> map = new HashMap<String, String>();

    /**
	 * @return the map
	 */
	public final HashMap<String, String> getMap() {
		return this.map;
	}

	/**
     * Constructs an object from the given initString using the given delimiters.
     *
     * @param initString
     * @throws IllegalArgumentException if the initString couldn't be parsed.
     */
    public PropertyMap(final String initString, final String pairDelimiter, final String keyValueDelimiter){
        this.initFromDelimitedString(initString, pairDelimiter, keyValueDelimiter);
    }

    /** Returns the value for the initialization property @a propName or the
     *  the passed default value @a default if no value for this property
     *  exists. */

    public final String getProperty(final String propName, final String defaultVal) {
        if (!this.initStringProcessed) {
            PropertyMap.log.error("InitString not yet processed. " +
                    " Call method initVarsFromInitString(..) first.");
            return null;
        }

        String retVal = this.map.get(propName);
        if (retVal == null) {
            retVal = defaultVal;
        }

        return retVal;
    }

    /** Returns the value for the initialization property @a propName or null
     *  if no value for this property exists. */

    public final String getProperty(final String propName) {
        return this.getProperty(propName, null);
    }
    private boolean initStringProcessed = false;

    /**
     * Parses the initialization string @a initString for this component.
     * The initilization string consists of key/value pairs.
     * After this method is executed, the parameter values can be retrieved
     * using the method getInitProperty(..).
     */

    private final void initFromDelimitedString(final String initString, final String pairDelimiter, final String keyValueDelimiter) throws IllegalArgumentException {
        if ((initString == null) || (initString.length() == 0)) {
            this.initStringProcessed = true;
            return;
        }

        try {
            final StringTokenizer keyValListTokens = new StringTokenizer(initString, pairDelimiter);
            while (keyValListTokens.hasMoreTokens()) {
                final String curKeyValToken = keyValListTokens.nextToken().trim();
                final StringTokenizer keyValTokens = new StringTokenizer(curKeyValToken, keyValueDelimiter);
                if (keyValTokens.countTokens() != 2) {
                    throw new IllegalArgumentException("Expected key=value pair, found " + curKeyValToken);
                }
                final String key = keyValTokens.nextToken().trim();
                final String val = keyValTokens.nextToken().trim();
                PropertyMap.log.info("Found key/value pair: " + key + "=" + val);
                this.map.put(key, val);
            }
        } catch (final Exception exc) {
            throw new IllegalArgumentException("Error parsing init string '" + initString + "'", exc);
        }

        this.initStringProcessed = true;
    }
}