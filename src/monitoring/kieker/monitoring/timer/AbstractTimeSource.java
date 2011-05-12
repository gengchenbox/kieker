package kieker.monitoring.timer;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import kieker.monitoring.core.configuration.Configuration;

public abstract class AbstractTimeSource implements ITimeSource {
	private static final Log log = LogFactory.getLog(AbstractTimeSource.class);

	protected final Configuration configuration; 

	protected AbstractTimeSource(final Configuration configuration) {
		try {
			// somewhat dirty hack...
			Properties defaultProps = getDefaultProperties();
			if (defaultProps != null) {
				configuration.setDefaultProperties(defaultProps);
			}
		} catch (final IllegalAccessException ex) {
			AbstractTimeSource.log.error("Unable to set timer custom default properties");
		}
		this.configuration = configuration;
	}
	
	/**
	 * This method should be overwritten, iff the timer is external to Kieker and
	 * thus its default configuration is not included in the default config file.
	 * 
	 * @return
	 */
	protected Properties getDefaultProperties() {
		return null;
	}
	
	@Override
	public abstract long getTime();
}