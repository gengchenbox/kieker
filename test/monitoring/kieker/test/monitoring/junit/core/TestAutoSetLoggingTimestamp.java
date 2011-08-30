/**
 * 
 */
package kieker.test.monitoring.junit.core;

import java.util.List;
import java.util.Properties;

import junit.framework.Assert;
import junit.framework.TestCase;
import kieker.common.record.IMonitoringRecord;
import kieker.monitoring.core.configuration.Configuration;
import kieker.monitoring.core.controller.IMonitoringController;
import kieker.test.monitoring.junit.util.DummyRecord;
import kieker.test.monitoring.junit.util.NamedPipeFactory;

/**
 * Tests, whether the property <i>setLoggingTimestamp</i> works properly.
 * 
 * @author Andre van Hoorn
 * 
 */
public class TestAutoSetLoggingTimestamp extends TestCase {

	private void testSetLoggingTimestamp(
			final boolean setLoggingTimestampEnabled) {
		final String pipeName = NamedPipeFactory.createPipeName();

		final Properties additionalConfigurationProperties;
		{ /* We will pass the property Configuration.AUTO_SET_LOGGINGTSTAMP as
		   * and additional configuration property */
			additionalConfigurationProperties = new Properties();
			if (setLoggingTimestampEnabled) {
				// auto set logging timestamps enabled
				additionalConfigurationProperties.put(
						Configuration.AUTO_SET_LOGGINGTSTAMP,
						Boolean.toString(true));
			} else {
				// auto set logging timestamps disabled
				additionalConfigurationProperties.put(
						Configuration.AUTO_SET_LOGGINGTSTAMP,
						Boolean.toString(false));
			}
		}

		final IMonitoringController monitoringController =
				NamedPipeFactory.createMonitoringControllerWithNamedPipe(
						pipeName, additionalConfigurationProperties);

		/*
		 * We will now register a custom IPipeReader which receives records
		 * through the pipe and collects these in a list. On purpose, we are not
		 * using the corresponding PipeReader that comes with Kieker.
		 */
		final List<IMonitoringRecord> receivedRecords =
				NamedPipeFactory
						.createAndRegisterNamedPipeRecordCollector(pipeName);

		final DummyRecord rSent = new DummyRecord();
		final long timestampSent = rSent.getLoggingTimestamp();
		monitoringController.newMonitoringRecord(new DummyRecord());

		Assert.assertEquals(
				"Test invalid: exactly one record should have been received",
				1, receivedRecords.size());

		// Note, that rSent and rReceived are actually the same, but
		// since this is implementation knowledge, we'll fetch the
		// logged record from the list
		final DummyRecord rReceived = (DummyRecord) receivedRecords.get(0);
		final long timestampReceived = rReceived.getLoggingTimestamp();

		if (setLoggingTimestampEnabled) {
			Assert.assertTrue(
					"The logging timestamps must not be equal since setLoggingTimestamp property is set true (timestampSent: "
							+ timestampSent
							+ "; "
							+ "timestampReceived: "
							+ timestampReceived,
					timestampSent != timestampReceived);
		} else {
			Assert.assertTrue(
					"The logging timestamps must be equal since setLoggingTimestamp property is set false (timestampSent: "
							+ timestampSent
							+ "; "
							+ "timestampReceived: "
							+ timestampReceived,
					timestampSent == timestampReceived);
		}
	}

	public void testSetLoggingTimestampEnabled() {
		this.testSetLoggingTimestamp(true);
	}

	public void testSetLoggingTimestampDisabled() {
		this.testSetLoggingTimestamp(false);
	}
}