/***************************************************************************
 * Copyright 2022 Kieker Project (http://kieker-monitoring.net)
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
 ***************************************************************************/

package kieker.analysis.tt.writeRead.filesystem;

import java.util.LinkedList;
import java.util.List;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import kieker.analysis.source.file.DirectoryReaderStage;
import kieker.analysis.tt.writeRead.TestDataRepository;
import kieker.analysis.tt.writeRead.TestProbe;
import kieker.common.configuration.Configuration;
import kieker.common.record.IMonitoringRecord;
import kieker.common.record.controlflow.OperationExecutionRecord;
import kieker.monitoring.core.configuration.ConfigurationConstants;
import kieker.monitoring.core.configuration.ConfigurationFactory;
import kieker.monitoring.core.controller.MonitoringController;
import kieker.monitoring.core.controller.WriterController;
import kieker.monitoring.writer.compression.NoneCompressionFilter;
import kieker.monitoring.writer.filesystem.FileWriter;

import teetime.framework.test.StageTester;

/**
 * @author Andre van Hoorn, Christian Wulf, Lars Bluemke
 *
 * @since 1.5
 */
public class AsciiSkipBrokenRecordsTest {

	private static final TestDataRepository TEST_DATA_REPOSITORY = new TestDataRepository();
	private static final int TIMEOUT_IN_MS = 0;

	@Rule
	public final TemporaryFolder tmpFolder = new TemporaryFolder(); // NOCS (@Rule must be public)

	public AsciiSkipBrokenRecordsTest() {
		super();
	}

	@Ignore
	@Test
	public void testSkipBrokenRecordsWithEnabledIgnore() throws Exception {
		final List<IMonitoringRecord> records = TEST_DATA_REPOSITORY.newTestUnknownRecords();

		final List<IMonitoringRecord> analyzedRecords = this.executeTestSetup(records, true);

		// we expect that EVENT1_UNKNOWN_TYPE and EVENT3_UNKNOWN_TYPE are simply ignored
		Assert.assertThat(analyzedRecords.get(0), CoreMatchers.is(CoreMatchers.equalTo(records.get(0))));
		Assert.assertThat(analyzedRecords.get(1), CoreMatchers.is(CoreMatchers.equalTo(records.get(2))));
		Assert.assertThat(analyzedRecords.size(), CoreMatchers.is(2));
	}

	@Ignore
	@Test
	public void testSkipBrokenRecordsWithDisabledIgnore() throws Exception {
		final List<IMonitoringRecord> records = TEST_DATA_REPOSITORY.newTestUnknownRecords();

		final List<IMonitoringRecord> analyzedRecords = this.executeTestSetup(records, false);

		// we expect that EVENT1_UNKNOWN_TYPE and EVENT3_UNKNOWN_TYPE are simply ignored
		Assert.assertThat(analyzedRecords.get(0), CoreMatchers.is(CoreMatchers.equalTo(records.get(0))));
		Assert.assertThat(analyzedRecords.get(1), CoreMatchers.is(CoreMatchers.equalTo(records.get(2))));
		Assert.assertThat(analyzedRecords.size(), CoreMatchers.is(2));
	}

	@SuppressWarnings("PMD.JUnit4TestShouldUseTestAnnotation")
	private List<IMonitoringRecord> executeTestSetup(final List<IMonitoringRecord> records, final boolean ignoreUnknownRecordTypes)
			throws Exception {
		// 2. define monitoring config
		final Configuration config = ConfigurationFactory.createDefaultConfiguration();
		config.setProperty(ConfigurationConstants.WRITER_CLASSNAME, FileWriter.class.getName());
		config.setProperty(WriterController.RECORD_QUEUE_SIZE, "128");
		config.setProperty(WriterController.RECORD_QUEUE_INSERT_BEHAVIOR, "1");
		config.setProperty(FileWriter.CONFIG_PATH, this.tmpFolder.getRoot().getCanonicalPath());
		config.setProperty(FileWriter.CONFIG_COMPRESSION_FILTER, NoneCompressionFilter.class.getName());
		final MonitoringController monitoringController = MonitoringController.createInstance(config);

		// 3. initialize the reader
		final String[] monitoringLogDirs = TEST_DATA_REPOSITORY.getAbsoluteMonitoringLogDirNames(this.tmpFolder.getRoot());

		final DirectoryReaderStage directoryReaderStage = new DirectoryReaderStage(true, 8192);
		final List<IMonitoringRecord> outputList = new LinkedList<>();

		// 4. trigger records
		final TestProbe testProbe = new TestProbe(monitoringController);
		Assert.assertTrue(monitoringController.isMonitoringEnabled());
		testProbe.triggerRecords(records);
		Assert.assertTrue(monitoringController.isMonitoringEnabled());

		// 5. terminate monitoring
		monitoringController.terminateMonitoring();

		// 6a. wait for termination: monitoring
		monitoringController.waitForTermination(TIMEOUT_IN_MS);

		// Instead of replacing the type entry by an invalid one, we replace by just another
		// valid type that does not match the type. We want to make sure that only these records
		// are ignored, but the readers processes the remaining part of the file.
		final String classnameToManipulate = records.get(1).getClass().getName();
		FileContentUtil.replaceStringInMapFiles(monitoringLogDirs, classnameToManipulate, OperationExecutionRecord.class.getName());

		// 6b. execute the reader in test configuration
		StageTester.test(directoryReaderStage).and().send(this.tmpFolder.getRoot()).to(directoryReaderStage.getInputPort())
				.receive(outputList).from(directoryReaderStage.getOutputPort()).start();

		// 7. return actual records (sublist is used to exclude the KiekerMetadataRecord sent by the monitoring controller)
		return outputList.subList(1, outputList.size());
	}
}
