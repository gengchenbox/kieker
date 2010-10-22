/**
 * 
 */
package kieker.test.monitoring.junit.core.state;

/*
 * ==================LICENCE=========================
 * Copyright 2006-2009 Kieker Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ==================================================
 */

import junit.framework.Assert;
import junit.framework.TestCase;
import kieker.monitoring.core.configuration.IMonitoringConfiguration;
import kieker.monitoring.core.state.IMonitoringControllerState;
import kieker.monitoring.core.state.MonitoringControllerState;
import kieker.test.monitoring.junit.core.configuration.util.DefaultConfigurationFactory;

/**
 * @author Andre van Hoorn
 * 
 */
public class TestMonitoringControllerStateConstruction extends TestCase {
	
	/**
	 * 
	 */
	public void testConstructionFromConfig() {
		final String name = "TheName";
		final IMonitoringConfiguration config = DefaultConfigurationFactory
				.createDefaultConfigWithDummyWriter(name);

		{
			/* Test with default values */
			final IMonitoringControllerState state = new MonitoringControllerState(
					config);
			Assert.assertEquals("monitoringEnabled values differ",
					config.isMonitoringEnabled(), state.isMonitoringEnabled());
			Assert.assertEquals("debugEnabled values differ",
					config.isDebugEnabled(), state.isDebugEnabled());
			Assert.assertEquals("hostName values differ", config.getHostName(),
					state.getHostName());
			Assert.assertSame("log writers differ",
					config.getMonitoringLogWriter(),
					state.getMonitoringLogWriter());
		}

		{
			/* Change values and try again */
			config.setDebugEnabled(!config.isDebugEnabled());
			config.setMonitoringEnabled(!config.isMonitoringEnabled());
			config.setHostName(config.getHostName() + "__");

			final IMonitoringControllerState state = new MonitoringControllerState(
					config);
			Assert.assertEquals("monitoringEnabled values differ",
					config.isMonitoringEnabled(), state.isMonitoringEnabled());
			Assert.assertEquals("debugEnabled values differ",
					config.isDebugEnabled(), state.isDebugEnabled());
			Assert.assertEquals("hostName values differ", config.getHostName(),
					state.getHostName());
			Assert.assertSame("log writers differ",
					config.getMonitoringLogWriter(),
					state.getMonitoringLogWriter());

		}
	}
}