/***************************************************************************
 * Copyright 2018 Kieker Project (http://kieker-monitoring.net)
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
package kieker.tools.trace.analysis;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import kieker.analysis.AnalysisController;
import kieker.analysis.analysisComponent.AbstractAnalysisComponent;
import kieker.analysis.exception.AnalysisConfigurationException;
import kieker.analysis.plugin.AbstractPlugin;
import kieker.analysis.plugin.filter.flow.EventRecordTraceReconstructionFilter;
import kieker.analysis.plugin.filter.flow.ThreadEvent2TraceEventFilter;
import kieker.analysis.plugin.filter.forward.StringBufferFilter;
import kieker.analysis.plugin.filter.select.TimestampFilter;
import kieker.analysis.plugin.filter.select.TraceIdFilter;
import kieker.analysis.plugin.reader.filesystem.FSReader;
import kieker.common.configuration.Configuration;
import kieker.tools.common.Convert;
import kieker.tools.trace.analysis.filter.AbstractGraphProducingFilter;
import kieker.tools.trace.analysis.filter.AbstractMessageTraceProcessingFilter;
import kieker.tools.trace.analysis.filter.AbstractTraceAnalysisFilter;
import kieker.tools.trace.analysis.filter.AbstractTraceProcessingFilter;
import kieker.tools.trace.analysis.filter.IGraphOutputtingFilter;
import kieker.tools.trace.analysis.filter.executionRecordTransformation.ExecutionRecordTransformationFilter;
import kieker.tools.trace.analysis.filter.flow.EventRecordTraceCounter;
import kieker.tools.trace.analysis.filter.flow.TraceEventRecords2ExecutionAndMessageTraceFilter;
import kieker.tools.trace.analysis.filter.systemModel.SystemModel2FileFilter;
import kieker.tools.trace.analysis.filter.traceFilter.TraceEquivalenceClassFilter;
import kieker.tools.trace.analysis.filter.traceFilter.TraceEquivalenceClassFilter.TraceEquivalenceClassModes;
import kieker.tools.trace.analysis.filter.traceReconstruction.TraceReconstructionFilter;
import kieker.tools.trace.analysis.filter.traceWriter.ExecutionTraceWriterFilter;
import kieker.tools.trace.analysis.filter.traceWriter.InvalidExecutionTraceWriterFilter;
import kieker.tools.trace.analysis.filter.traceWriter.MessageTraceWriterFilter;
import kieker.tools.trace.analysis.filter.visualization.AbstractGraphFilter;
import kieker.tools.trace.analysis.filter.visualization.GraphWriterPlugin;
import kieker.tools.trace.analysis.filter.visualization.callTree.AbstractAggregatedCallTreeFilter;
import kieker.tools.trace.analysis.filter.visualization.callTree.AggregatedAllocationComponentOperationCallTreeFilter;
import kieker.tools.trace.analysis.filter.visualization.callTree.AggregatedAssemblyComponentOperationCallTreeFilter;
import kieker.tools.trace.analysis.filter.visualization.callTree.TraceCallTreeFilter;
import kieker.tools.trace.analysis.filter.visualization.dependencyGraph.AbstractDependencyGraphFilter;
import kieker.tools.trace.analysis.filter.visualization.dependencyGraph.ComponentDependencyGraphAllocationFilter;
import kieker.tools.trace.analysis.filter.visualization.dependencyGraph.ComponentDependencyGraphAssemblyFilter;
import kieker.tools.trace.analysis.filter.visualization.dependencyGraph.ContainerDependencyGraphFilter;
import kieker.tools.trace.analysis.filter.visualization.dependencyGraph.OperationDependencyGraphAllocationFilter;
import kieker.tools.trace.analysis.filter.visualization.dependencyGraph.OperationDependencyGraphAssemblyFilter;
import kieker.tools.trace.analysis.filter.visualization.dependencyGraph.ResponseTimeColorNodeDecorator;
import kieker.tools.trace.analysis.filter.visualization.dependencyGraph.ResponseTimeNodeDecorator;
import kieker.tools.trace.analysis.filter.visualization.descriptions.DescriptionDecoratorFilter;
import kieker.tools.trace.analysis.filter.visualization.sequenceDiagram.SequenceDiagramFilter;
import kieker.tools.trace.analysis.filter.visualization.traceColoring.TraceColoringFilter;
import kieker.tools.trace.analysis.repository.DescriptionRepository;
import kieker.tools.trace.analysis.repository.TraceColorRepository;
import kieker.tools.trace.analysis.systemModel.ExecutionTrace;
import kieker.tools.trace.analysis.systemModel.repository.SystemModelRepository;

/**
 * This is the main class to start the Kieker TraceAnalysisTool - the model synthesis and analysis tool to process the
 * monitoring data that comes from the instrumented system, or from a file that contains Kieker monitoring data. The
 * Kieker TraceAnalysisTool can produce output such as sequence diagrams, dependency graphs on demand. Alternatively it
 * can be used continuously for online performance analysis, anomaly detection or live visualization of system behavior.
 *
 * @author Andre van Hoorn, Matthias Rohr, Nils Christian Ehmke, Reiner Jung
 *
 * @since 0.95a
 */
public class PerformAnalysis {

	private static final String ENCODING = "UTF-8";
	private final AnalysisController analysisController = new AnalysisController();

	private final Logger logger;
	private final TraceAnalysisConfiguration settings;

	public PerformAnalysis(final Logger logger, final TraceAnalysisConfiguration settings) {
		this.logger = logger;
		this.settings = settings;
	}

	/**
	 *
	 * @return false iff an error occurred
	 */
	boolean dispatchTasks() {
		final String pathPrefix = this.settings.getOutputDir() + File.separator + this.settings.getPrefix();
		boolean retVal = true;
		int numRequestedTasks = 0;

		final SystemModelRepository systemEntityFactory = new SystemModelRepository(new Configuration(),
				this.analysisController);

		TraceReconstructionFilter mtReconstrFilter = null;
		EventRecordTraceCounter eventRecordTraceCounter = null;
		EventRecordTraceReconstructionFilter eventTraceReconstructionFilter = null;
		TraceEventRecords2ExecutionAndMessageTraceFilter traceEvents2ExecutionAndMessageTraceFilter = null;
		try {
			final FSReader reader;
			{ // NOCS (NestedBlock)
				final Configuration conf = new Configuration(null);
				conf.setProperty(FSReader.CONFIG_PROPERTY_NAME_INPUTDIRS, Configuration.toProperty(Convert.fileListToStringArray(this.settings.getInputDirs())));
				conf.setProperty(FSReader.CONFIG_PROPERTY_NAME_IGNORE_UNKNOWN_RECORD_TYPES, Boolean.TRUE.toString());
				reader = new FSReader(conf, this.analysisController);
			}

			// Unify Strings
			final StringBufferFilter stringBufferFilter = new StringBufferFilter(new Configuration(),
					this.analysisController);
			this.analysisController.connect(reader, FSReader.OUTPUT_PORT_NAME_RECORDS, stringBufferFilter,
					StringBufferFilter.INPUT_PORT_NAME_EVENTS);

			AbstractPlugin sourceStage;
			String sourcePort;

			sourceStage = stringBufferFilter;
			sourcePort = StringBufferFilter.OUTPUT_PORT_NAME_RELAYED_EVENTS;

			// transforms thread-based events to trace-based events
			final Configuration config = new Configuration();
			final ThreadEvent2TraceEventFilter threadEvent2TraceEventFilter = new ThreadEvent2TraceEventFilter(config,
					this.analysisController);
			this.analysisController.connect(sourceStage, sourcePort, threadEvent2TraceEventFilter,
					ThreadEvent2TraceEventFilter.INPUT_PORT_NAME_DEFAULT);

			sourceStage = threadEvent2TraceEventFilter;
			sourcePort = ThreadEvent2TraceEventFilter.OUTPUT_PORT_NAME_DEFAULT;

			// This map can be used within the constructor for all following plugins which use the repository with the
			// name defined in the
			// AbstractTraceAnalysisPlugin.
			final TimestampFilter timestampFilter;
			{ // NOCS (nested block)
				// Create the timestamp filter and connect to the reader's output port
				final Configuration configTimestampFilter = new Configuration();
				configTimestampFilter.setProperty(TimestampFilter.CONFIG_PROPERTY_NAME_IGNORE_BEFORE_TIMESTAMP,
						this.longToString(this.settings.getIgnoreExecutionsBeforeDate()));

				configTimestampFilter.setProperty(TimestampFilter.CONFIG_PROPERTY_NAME_IGNORE_AFTER_TIMESTAMP,
						this.longToString(this.settings.getIgnoreExecutionsAfterDate()));

				timestampFilter = new TimestampFilter(configTimestampFilter, this.analysisController);
				this.analysisController.connect(sourceStage, sourcePort, timestampFilter,
						TimestampFilter.INPUT_PORT_NAME_EXECUTION);
				this.analysisController.connect(sourceStage, sourcePort, timestampFilter,
						TimestampFilter.INPUT_PORT_NAME_FLOW);
			}

			final TraceIdFilter traceIdFilter;
			{ // NOCS (nested block)
				// Create the trace ID filter and connect to the timestamp filter's output port
				final Configuration configTraceIdFilterFlow = new Configuration();
				if (this.settings.getSelectedTraces() == null) {
					configTraceIdFilterFlow.setProperty(TraceIdFilter.CONFIG_PROPERTY_NAME_SELECT_ALL_TRACES,
							Boolean.TRUE.toString());
				} else {
					configTraceIdFilterFlow.setProperty(TraceIdFilter.CONFIG_PROPERTY_NAME_SELECT_ALL_TRACES,
							Boolean.FALSE.toString());
					configTraceIdFilterFlow.setProperty(TraceIdFilter.CONFIG_PROPERTY_NAME_SELECTED_TRACES,
							Configuration
									.toProperty(this.settings.getSelectedTraces().toArray(new Long[this.settings.getSelectedTraces().size()])));
				}

				traceIdFilter = new TraceIdFilter(configTraceIdFilterFlow, this.analysisController);

				this.analysisController.connect(timestampFilter, TimestampFilter.OUTPUT_PORT_NAME_WITHIN_PERIOD,
						traceIdFilter, TraceIdFilter.INPUT_PORT_NAME_COMBINED);
			}

			final ExecutionRecordTransformationFilter execRecTransformer;
			{ // NOCS (nested block)
				// Create the execution record transformation filter and connect to the trace ID filter's output port
				final Configuration execRecTransformerConfig = new Configuration();
				execRecTransformerConfig.setProperty(AbstractAnalysisComponent.CONFIG_NAME,
						Constants.EXEC_TRACE_RECONSTR_COMPONENT_NAME);
				execRecTransformer = new ExecutionRecordTransformationFilter(execRecTransformerConfig,
						this.analysisController);
				if (this.settings.isInvertTraceIdFilter()) {
					this.analysisController.connect(traceIdFilter, TraceIdFilter.OUTPUT_PORT_NAME_MISMATCH,
							execRecTransformer, ExecutionRecordTransformationFilter.INPUT_PORT_NAME_RECORDS);
				} else {
					this.analysisController.connect(traceIdFilter, TraceIdFilter.OUTPUT_PORT_NAME_MATCH,
							execRecTransformer, ExecutionRecordTransformationFilter.INPUT_PORT_NAME_RECORDS);
				}

				this.analysisController.connect(execRecTransformer,
						AbstractTraceAnalysisFilter.REPOSITORY_PORT_NAME_SYSTEM_MODEL, systemEntityFactory);
			}

			{ // NOCS (nested block)
				// Create the trace reconstruction filter and connect to the record transformation filter's output port
				final Configuration mtReconstrFilterConfig = new Configuration();
				mtReconstrFilterConfig.setProperty(AbstractAnalysisComponent.CONFIG_NAME,
						Constants.TRACERECONSTR_COMPONENT_NAME);
				mtReconstrFilterConfig.setProperty(TraceReconstructionFilter.CONFIG_PROPERTY_NAME_TIMEUNIT,
						TimeUnit.MILLISECONDS.name());
				mtReconstrFilterConfig.setProperty(TraceReconstructionFilter.CONFIG_PROPERTY_NAME_MAX_TRACE_DURATION,
						this.longToString(this.settings.getMaxTraceDuration()));
				mtReconstrFilterConfig.setProperty(TraceReconstructionFilter.CONFIG_PROPERTY_NAME_IGNORE_INVALID_TRACES,
						this.booleanToString(this.settings.isIgnoreInvalidTraces()));
				mtReconstrFilter = new TraceReconstructionFilter(mtReconstrFilterConfig, this.analysisController);
				this.analysisController.connect(mtReconstrFilter,
						AbstractTraceAnalysisFilter.REPOSITORY_PORT_NAME_SYSTEM_MODEL, systemEntityFactory);
				this.analysisController.connect(execRecTransformer,
						ExecutionRecordTransformationFilter.OUTPUT_PORT_NAME_EXECUTIONS, mtReconstrFilter,
						TraceReconstructionFilter.INPUT_PORT_NAME_EXECUTIONS);
			}

			{ // NOCS (nested block)
				// Create the event record trace generation filter and connect to the trace ID filter's output port
				final Configuration configurationEventRecordTraceGenerationFilter = new Configuration();
				configurationEventRecordTraceGenerationFilter.setProperty(AbstractAnalysisComponent.CONFIG_NAME,
						Constants.EVENTRECORDTRACERECONSTR_COMPONENT_NAME);
				configurationEventRecordTraceGenerationFilter.setProperty(
						EventRecordTraceReconstructionFilter.CONFIG_PROPERTY_NAME_TIMEUNIT,
						TimeUnit.MILLISECONDS.name());
				configurationEventRecordTraceGenerationFilter.setProperty(
						EventRecordTraceReconstructionFilter.CONFIG_PROPERTY_NAME_MAX_TRACE_DURATION,
						this.longToString(this.settings.getMaxTraceDuration()));
				configurationEventRecordTraceGenerationFilter.setProperty(
						EventRecordTraceReconstructionFilter.CONFIG_PROPERTY_NAME_REPAIR_EVENT_BASED_TRACES,
						this.booleanToString(this.settings.isRepairEventBasedTraces()));
				eventTraceReconstructionFilter = new EventRecordTraceReconstructionFilter(
						configurationEventRecordTraceGenerationFilter, this.analysisController);

				final String outputPortName;
				if (this.settings.isInvertTraceIdFilter()) {
					outputPortName = TraceIdFilter.OUTPUT_PORT_NAME_MISMATCH;
				} else {
					outputPortName = TraceIdFilter.OUTPUT_PORT_NAME_MATCH;
				}
				this.analysisController.connect(traceIdFilter, outputPortName, eventTraceReconstructionFilter,
						EventRecordTraceReconstructionFilter.INPUT_PORT_NAME_TRACE_RECORDS);
			}

			{ // NOCS (nested block)
				// Create the counter for valid/invalid event record traces
				final Configuration configurationEventRecordTraceCounter = new Configuration();
				configurationEventRecordTraceCounter.setProperty(AbstractAnalysisComponent.CONFIG_NAME,
						Constants.EXECEVENTRACESFROMEVENTTRACES_COMPONENT_NAME);
				configurationEventRecordTraceCounter.setProperty(
						EventRecordTraceCounter.CONFIG_PROPERTY_NAME_LOG_INVALID,
						this.booleanToString(!this.settings.isIgnoreInvalidTraces()));
				eventRecordTraceCounter = new EventRecordTraceCounter(configurationEventRecordTraceCounter,
						this.analysisController);

				this.analysisController.connect(eventTraceReconstructionFilter,
						EventRecordTraceReconstructionFilter.OUTPUT_PORT_NAME_TRACE_VALID, eventRecordTraceCounter,
						EventRecordTraceCounter.INPUT_PORT_NAME_VALID);
				this.analysisController.connect(eventTraceReconstructionFilter,
						EventRecordTraceReconstructionFilter.OUTPUT_PORT_NAME_TRACE_INVALID, eventRecordTraceCounter,
						EventRecordTraceCounter.INPUT_PORT_NAME_INVALID);
			}

			{ // NOCS (nested block)
				// Create the event trace to execution/message trace transformation filter and connect its input to the
				// event record trace generation filter's output
				// port
				final Configuration configurationEventTrace2ExecutionTraceFilter = new Configuration();
				configurationEventTrace2ExecutionTraceFilter.setProperty(AbstractAnalysisComponent.CONFIG_NAME,
						Constants.EXECTRACESFROMEVENTTRACES_COMPONENT_NAME);
				configurationEventTrace2ExecutionTraceFilter.setProperty(
						TraceEventRecords2ExecutionAndMessageTraceFilter.CONFIG_IGNORE_ASSUMED,
						this.booleanToString(this.settings.isIgnoreAssumedCalls()));
				// EventTrace2ExecutionTraceFilter has no configuration properties
				traceEvents2ExecutionAndMessageTraceFilter = new TraceEventRecords2ExecutionAndMessageTraceFilter(
						configurationEventTrace2ExecutionTraceFilter, this.analysisController);

				this.analysisController.connect(eventTraceReconstructionFilter,
						EventRecordTraceReconstructionFilter.OUTPUT_PORT_NAME_TRACE_VALID,
						traceEvents2ExecutionAndMessageTraceFilter,
						TraceEventRecords2ExecutionAndMessageTraceFilter.INPUT_PORT_NAME_EVENT_TRACE);
				this.analysisController.connect(traceEvents2ExecutionAndMessageTraceFilter,
						AbstractTraceAnalysisFilter.REPOSITORY_PORT_NAME_SYSTEM_MODEL, systemEntityFactory);
			}

			final List<AbstractTraceProcessingFilter> allTraceProcessingComponents = new ArrayList<>();
			final List<AbstractGraphProducingFilter<?>> allGraphProducers = new ArrayList<>();

			final Configuration traceAllocationEquivClassFilterConfig = new Configuration();
			traceAllocationEquivClassFilterConfig.setProperty(AbstractAnalysisComponent.CONFIG_NAME,
					Constants.TRACEALLOCATIONEQUIVCLASS_COMPONENT_NAME);
			traceAllocationEquivClassFilterConfig.setProperty(
					TraceEquivalenceClassFilter.CONFIG_PROPERTY_NAME_EQUIVALENCE_MODE,
					TraceEquivalenceClassModes.ALLOCATION.toString());
			TraceEquivalenceClassFilter traceAllocationEquivClassFilter = null; // must not be instantiate it here, due
																				// to side-effects in the constructor
			if (this.settings.isPrintDeploymentEquivalenceClasses()) {
				/**
				 * Currently, this filter is only used to print an equivalence report. That's why we only activate it in
				 * case this options is requested.
				 */
				traceAllocationEquivClassFilter = new TraceEquivalenceClassFilter(traceAllocationEquivClassFilterConfig,
						this.analysisController);
				this.analysisController.connect(traceAllocationEquivClassFilter,
						AbstractTraceAnalysisFilter.REPOSITORY_PORT_NAME_SYSTEM_MODEL, systemEntityFactory);
				this.analysisController.connect(mtReconstrFilter,
						TraceReconstructionFilter.OUTPUT_PORT_NAME_EXECUTION_TRACE, traceAllocationEquivClassFilter,
						TraceEquivalenceClassFilter.INPUT_PORT_NAME_EXECUTION_TRACE);
				this.analysisController.connect(traceEvents2ExecutionAndMessageTraceFilter,
						TraceEventRecords2ExecutionAndMessageTraceFilter.OUTPUT_PORT_NAME_EXECUTION_TRACE,
						traceAllocationEquivClassFilter, TraceEquivalenceClassFilter.INPUT_PORT_NAME_EXECUTION_TRACE);
				allTraceProcessingComponents.add(traceAllocationEquivClassFilter);
			}

			final Configuration traceAssemblyEquivClassFilterConfig = new Configuration();
			traceAssemblyEquivClassFilterConfig.setProperty(AbstractAnalysisComponent.CONFIG_NAME,
					Constants.TRACEASSEMBLYEQUIVCLASS_COMPONENT_NAME);
			traceAssemblyEquivClassFilterConfig.setProperty(
					TraceEquivalenceClassFilter.CONFIG_PROPERTY_NAME_EQUIVALENCE_MODE,
					TraceEquivalenceClassModes.ASSEMBLY.toString());
			TraceEquivalenceClassFilter traceAssemblyEquivClassFilter = null; // must not be instantiate it here, due to
																				// side-effects in the constructor
			if (this.settings.isPrintAssemblyEquivalenceClasses()) {
				/**
				 * Currently, this filter is only used to print an equivalence report. That's why we only activate it in
				 * case this options is requested.
				 */
				traceAssemblyEquivClassFilter = new TraceEquivalenceClassFilter(traceAssemblyEquivClassFilterConfig,
						this.analysisController);
				this.analysisController.connect(mtReconstrFilter,
						TraceReconstructionFilter.OUTPUT_PORT_NAME_EXECUTION_TRACE, traceAssemblyEquivClassFilter,
						TraceEquivalenceClassFilter.INPUT_PORT_NAME_EXECUTION_TRACE);
				this.analysisController.connect(traceEvents2ExecutionAndMessageTraceFilter,
						TraceEventRecords2ExecutionAndMessageTraceFilter.OUTPUT_PORT_NAME_EXECUTION_TRACE,
						traceAssemblyEquivClassFilter, TraceEquivalenceClassFilter.INPUT_PORT_NAME_EXECUTION_TRACE);
				this.analysisController.connect(traceAssemblyEquivClassFilter,
						AbstractTraceAnalysisFilter.REPOSITORY_PORT_NAME_SYSTEM_MODEL, systemEntityFactory);
				allTraceProcessingComponents.add(traceAssemblyEquivClassFilter);
			}

			// fill list of msgTraceProcessingComponents:
			MessageTraceWriterFilter componentPrintMsgTrace = null;
			if (this.settings.isPrintMessageTraces()) {
				numRequestedTasks++;
				final Configuration componentPrintMsgTraceConfig = new Configuration();
				componentPrintMsgTraceConfig.setProperty(AbstractAnalysisComponent.CONFIG_NAME,
						Constants.PRINTMSGTRACE_COMPONENT_NAME);
				componentPrintMsgTraceConfig.setProperty(MessageTraceWriterFilter.CONFIG_PROPERTY_NAME_OUTPUT_FN,
						new File(pathPrefix + Constants.MESSAGE_TRACES_FN_PREFIX + ".txt").getCanonicalPath());
				componentPrintMsgTrace = new MessageTraceWriterFilter(componentPrintMsgTraceConfig,
						this.analysisController);

				this.analysisController.connect(mtReconstrFilter,
						TraceReconstructionFilter.OUTPUT_PORT_NAME_MESSAGE_TRACE, componentPrintMsgTrace,
						AbstractMessageTraceProcessingFilter.INPUT_PORT_NAME_MESSAGE_TRACES);
				this.analysisController.connect(traceEvents2ExecutionAndMessageTraceFilter,
						TraceEventRecords2ExecutionAndMessageTraceFilter.OUTPUT_PORT_NAME_MESSAGE_TRACE,
						componentPrintMsgTrace, AbstractMessageTraceProcessingFilter.INPUT_PORT_NAME_MESSAGE_TRACES);
				this.analysisController.connect(componentPrintMsgTrace,
						AbstractTraceAnalysisFilter.REPOSITORY_PORT_NAME_SYSTEM_MODEL, systemEntityFactory);
				allTraceProcessingComponents.add(componentPrintMsgTrace);
			}
			ExecutionTraceWriterFilter componentPrintExecTrace = null;
			if (this.settings.isPrintExecutionTraces()) {
				numRequestedTasks++;
				final Configuration componentPrintExecTraceConfig = new Configuration();
				componentPrintExecTraceConfig.setProperty(AbstractAnalysisComponent.CONFIG_NAME,
						Constants.PRINTEXECTRACE_COMPONENT_NAME);
				componentPrintExecTraceConfig.setProperty(ExecutionTraceWriterFilter.CONFIG_PROPERTY_NAME_OUTPUT_FN,
						new File(pathPrefix + Constants.EXECUTION_TRACES_FN_PREFIX + ".txt").getCanonicalPath());
				componentPrintExecTrace = new ExecutionTraceWriterFilter(componentPrintExecTraceConfig,
						this.analysisController);

				this.analysisController.connect(mtReconstrFilter,
						TraceReconstructionFilter.OUTPUT_PORT_NAME_EXECUTION_TRACE, componentPrintExecTrace,
						ExecutionTraceWriterFilter.INPUT_PORT_NAME_EXECUTION_TRACES);
				this.analysisController.connect(traceEvents2ExecutionAndMessageTraceFilter,
						TraceEventRecords2ExecutionAndMessageTraceFilter.OUTPUT_PORT_NAME_EXECUTION_TRACE,
						componentPrintExecTrace, ExecutionTraceWriterFilter.INPUT_PORT_NAME_EXECUTION_TRACES);
				this.analysisController.connect(componentPrintExecTrace,
						AbstractTraceAnalysisFilter.REPOSITORY_PORT_NAME_SYSTEM_MODEL, systemEntityFactory);
				allTraceProcessingComponents.add(componentPrintExecTrace);
			}
			InvalidExecutionTraceWriterFilter componentPrintInvalidTrace = null;
			if (this.settings.isPrintInvalidExecutionTraces()) {
				numRequestedTasks++;
				final Configuration componentPrintInvalidTraceConfig = new Configuration();
				componentPrintInvalidTraceConfig.setProperty(AbstractAnalysisComponent.CONFIG_NAME,
						Constants.PRINTINVALIDEXECTRACE_COMPONENT_NAME);
				componentPrintInvalidTraceConfig.setProperty(
						InvalidExecutionTraceWriterFilter.CONFIG_PROPERTY_NAME_OUTPUT_FN,
						new File(pathPrefix + Constants.INVALID_TRACES_FN_PREFIX + ".txt").getCanonicalPath());
				componentPrintInvalidTrace = new InvalidExecutionTraceWriterFilter(componentPrintInvalidTraceConfig,
						this.analysisController);

				this.analysisController.connect(mtReconstrFilter,
						TraceReconstructionFilter.OUTPUT_PORT_NAME_INVALID_EXECUTION_TRACE, componentPrintInvalidTrace,
						InvalidExecutionTraceWriterFilter.INPUT_PORT_NAME_INVALID_EXECUTION_TRACES);
				this.analysisController.connect(componentPrintInvalidTrace,
						AbstractTraceAnalysisFilter.REPOSITORY_PORT_NAME_SYSTEM_MODEL, systemEntityFactory);
				this.analysisController.connect(traceEvents2ExecutionAndMessageTraceFilter,
						TraceEventRecords2ExecutionAndMessageTraceFilter.OUTPUT_PORT_NAME_INVALID_EXECUTION_TRACE,
						componentPrintInvalidTrace,
						InvalidExecutionTraceWriterFilter.INPUT_PORT_NAME_INVALID_EXECUTION_TRACES);
				allTraceProcessingComponents.add(componentPrintInvalidTrace);
			}
			SequenceDiagramFilter componentPlotAllocationSeqDiagr = null;
			if (retVal && this.settings.isPlotDeploymentSequenceDiagrams()) {
				numRequestedTasks++;
				final Configuration componentPlotAllocationSeqDiagrConfig = new Configuration();
				componentPlotAllocationSeqDiagrConfig.setProperty(AbstractAnalysisComponent.CONFIG_NAME,
						Constants.PLOTALLOCATIONSEQDIAGR_COMPONENT_NAME);
				componentPlotAllocationSeqDiagrConfig.setProperty(
						SequenceDiagramFilter.CONFIG_PROPERTY_NAME_OUTPUT_FN_BASE, pathPrefix + Constants.ALLOCATION_SEQUENCE_DIAGRAM_FN_PREFIX);
				componentPlotAllocationSeqDiagrConfig.setProperty(
						SequenceDiagramFilter.CONFIG_PROPERTY_NAME_OUTPUT_SDMODE,
						SequenceDiagramFilter.SDModes.ALLOCATION.toString());
				componentPlotAllocationSeqDiagrConfig.setProperty(
						SequenceDiagramFilter.CONFIG_PROPERTY_NAME_OUTPUT_SHORTLABES,
						this.booleanToString(this.settings.isShortLabels()));
				componentPlotAllocationSeqDiagr = new SequenceDiagramFilter(componentPlotAllocationSeqDiagrConfig,
						this.analysisController);

				this.analysisController.connect(mtReconstrFilter,
						TraceReconstructionFilter.OUTPUT_PORT_NAME_MESSAGE_TRACE, componentPlotAllocationSeqDiagr,
						AbstractMessageTraceProcessingFilter.INPUT_PORT_NAME_MESSAGE_TRACES);
				this.analysisController.connect(traceEvents2ExecutionAndMessageTraceFilter,
						TraceEventRecords2ExecutionAndMessageTraceFilter.OUTPUT_PORT_NAME_MESSAGE_TRACE,
						componentPlotAllocationSeqDiagr,
						AbstractMessageTraceProcessingFilter.INPUT_PORT_NAME_MESSAGE_TRACES);
				this.analysisController.connect(componentPlotAllocationSeqDiagr,
						AbstractTraceAnalysisFilter.REPOSITORY_PORT_NAME_SYSTEM_MODEL, systemEntityFactory);
				allTraceProcessingComponents.add(componentPlotAllocationSeqDiagr);
			}
			SequenceDiagramFilter componentPlotAssemblySeqDiagr = null;
			if (retVal && this.settings.isPlotAssemblySequenceDiagrams()) {
				numRequestedTasks++;
				final Configuration componentPlotAssemblySeqDiagrConfig = new Configuration();
				componentPlotAssemblySeqDiagrConfig.setProperty(AbstractAnalysisComponent.CONFIG_NAME,
						Constants.PLOTASSEMBLYSEQDIAGR_COMPONENT_NAME);
				componentPlotAssemblySeqDiagrConfig
						.setProperty(SequenceDiagramFilter.CONFIG_PROPERTY_NAME_OUTPUT_FN_BASE, pathPrefix + Constants.ASSEMBLY_SEQUENCE_DIAGRAM_FN_PREFIX);
				componentPlotAssemblySeqDiagrConfig.setProperty(
						SequenceDiagramFilter.CONFIG_PROPERTY_NAME_OUTPUT_SDMODE,
						SequenceDiagramFilter.SDModes.ASSEMBLY.toString());
				componentPlotAssemblySeqDiagrConfig.setProperty(
						SequenceDiagramFilter.CONFIG_PROPERTY_NAME_OUTPUT_SHORTLABES,
						this.booleanToString(this.settings.isShortLabels()));
				componentPlotAssemblySeqDiagr = new SequenceDiagramFilter(componentPlotAssemblySeqDiagrConfig,
						this.analysisController);

				this.analysisController.connect(mtReconstrFilter,
						TraceReconstructionFilter.OUTPUT_PORT_NAME_MESSAGE_TRACE, componentPlotAssemblySeqDiagr,
						AbstractMessageTraceProcessingFilter.INPUT_PORT_NAME_MESSAGE_TRACES);
				this.analysisController.connect(traceEvents2ExecutionAndMessageTraceFilter,
						TraceEventRecords2ExecutionAndMessageTraceFilter.OUTPUT_PORT_NAME_MESSAGE_TRACE,
						componentPlotAssemblySeqDiagr,
						AbstractMessageTraceProcessingFilter.INPUT_PORT_NAME_MESSAGE_TRACES);
				this.analysisController.connect(componentPlotAssemblySeqDiagr,
						AbstractTraceAnalysisFilter.REPOSITORY_PORT_NAME_SYSTEM_MODEL, systemEntityFactory);
				allTraceProcessingComponents.add(componentPlotAssemblySeqDiagr);
			}

			ComponentDependencyGraphAllocationFilter componentPlotAllocationComponentDepGraph = null;
			if (retVal && !this.settings.getPlotDeploymentComponentDependencyGraph().isEmpty()) {
				numRequestedTasks++;
				final Configuration configuration = new Configuration();
				componentPlotAllocationComponentDepGraph = new ComponentDependencyGraphAllocationFilter(configuration,
						this.analysisController);

				final String[] nodeDecorations = Convert.listToArray(this.settings.getPlotDeploymentComponentDependencyGraph());
				this.addDecorators(nodeDecorations, componentPlotAllocationComponentDepGraph);

				this.analysisController.connect(mtReconstrFilter,
						TraceReconstructionFilter.OUTPUT_PORT_NAME_MESSAGE_TRACE,
						componentPlotAllocationComponentDepGraph,
						AbstractMessageTraceProcessingFilter.INPUT_PORT_NAME_MESSAGE_TRACES);
				this.analysisController.connect(traceEvents2ExecutionAndMessageTraceFilter,
						TraceEventRecords2ExecutionAndMessageTraceFilter.OUTPUT_PORT_NAME_MESSAGE_TRACE,
						componentPlotAllocationComponentDepGraph,
						AbstractMessageTraceProcessingFilter.INPUT_PORT_NAME_MESSAGE_TRACES);
				this.analysisController.connect(componentPlotAllocationComponentDepGraph,
						AbstractTraceAnalysisFilter.REPOSITORY_PORT_NAME_SYSTEM_MODEL, systemEntityFactory);

				allTraceProcessingComponents.add(componentPlotAllocationComponentDepGraph);
				allGraphProducers.add(componentPlotAllocationComponentDepGraph);
			}

			ComponentDependencyGraphAssemblyFilter componentPlotAssemblyComponentDepGraph = null;
			if (retVal && !this.settings.getPlotAssemblyComponentDependencyGraph().isEmpty()) {
				numRequestedTasks++;
				final Configuration configuration = new Configuration();
				componentPlotAssemblyComponentDepGraph = new ComponentDependencyGraphAssemblyFilter(configuration,
						this.analysisController);

				final String[] nodeDecorations = Convert.listToArray(this.settings.getPlotAssemblyComponentDependencyGraph());
				this.addDecorators(nodeDecorations, componentPlotAssemblyComponentDepGraph);

				this.analysisController.connect(mtReconstrFilter,
						TraceReconstructionFilter.OUTPUT_PORT_NAME_MESSAGE_TRACE,
						componentPlotAssemblyComponentDepGraph,
						AbstractMessageTraceProcessingFilter.INPUT_PORT_NAME_MESSAGE_TRACES);
				this.analysisController.connect(traceEvents2ExecutionAndMessageTraceFilter,
						TraceEventRecords2ExecutionAndMessageTraceFilter.OUTPUT_PORT_NAME_MESSAGE_TRACE,
						componentPlotAssemblyComponentDepGraph,
						AbstractMessageTraceProcessingFilter.INPUT_PORT_NAME_MESSAGE_TRACES);
				this.analysisController.connect(componentPlotAssemblyComponentDepGraph,
						AbstractTraceAnalysisFilter.REPOSITORY_PORT_NAME_SYSTEM_MODEL, systemEntityFactory);
				allTraceProcessingComponents.add(componentPlotAssemblyComponentDepGraph);
				allGraphProducers.add(componentPlotAssemblyComponentDepGraph);
			}

			ContainerDependencyGraphFilter componentPlotContainerDepGraph = null;
			if (retVal && this.settings.isPlotContainerDependencyGraph()) {
				numRequestedTasks++;
				final Configuration configuration = new Configuration();
				componentPlotContainerDepGraph = new ContainerDependencyGraphFilter(configuration,
						this.analysisController);
				this.analysisController.connect(mtReconstrFilter,
						TraceReconstructionFilter.OUTPUT_PORT_NAME_MESSAGE_TRACE, componentPlotContainerDepGraph,
						AbstractMessageTraceProcessingFilter.INPUT_PORT_NAME_MESSAGE_TRACES);
				this.analysisController.connect(traceEvents2ExecutionAndMessageTraceFilter,
						TraceEventRecords2ExecutionAndMessageTraceFilter.OUTPUT_PORT_NAME_MESSAGE_TRACE,
						componentPlotContainerDepGraph,
						AbstractMessageTraceProcessingFilter.INPUT_PORT_NAME_MESSAGE_TRACES);
				this.analysisController.connect(componentPlotContainerDepGraph,
						AbstractTraceAnalysisFilter.REPOSITORY_PORT_NAME_SYSTEM_MODEL, systemEntityFactory);
				allTraceProcessingComponents.add(componentPlotContainerDepGraph);
				allGraphProducers.add(componentPlotContainerDepGraph);
			}

			OperationDependencyGraphAllocationFilter componentPlotAllocationOperationDepGraph = null;
			if (retVal && !this.settings.getPlotDeploymentOperationDependencyGraph().isEmpty()) {
				numRequestedTasks++;
				final Configuration configuration = new Configuration();
				componentPlotAllocationOperationDepGraph = new OperationDependencyGraphAllocationFilter(configuration,
						this.analysisController);

				final String[] nodeDecorations = Convert.listToArray(this.settings.getPlotDeploymentOperationDependencyGraph());
				this.addDecorators(nodeDecorations, componentPlotAllocationOperationDepGraph);

				this.analysisController.connect(mtReconstrFilter,
						TraceReconstructionFilter.OUTPUT_PORT_NAME_MESSAGE_TRACE,
						componentPlotAllocationOperationDepGraph,
						AbstractMessageTraceProcessingFilter.INPUT_PORT_NAME_MESSAGE_TRACES);
				this.analysisController.connect(traceEvents2ExecutionAndMessageTraceFilter,
						TraceEventRecords2ExecutionAndMessageTraceFilter.OUTPUT_PORT_NAME_MESSAGE_TRACE,
						componentPlotAllocationOperationDepGraph,
						AbstractMessageTraceProcessingFilter.INPUT_PORT_NAME_MESSAGE_TRACES);
				this.analysisController.connect(componentPlotAllocationOperationDepGraph,
						AbstractTraceAnalysisFilter.REPOSITORY_PORT_NAME_SYSTEM_MODEL, systemEntityFactory);
				allTraceProcessingComponents.add(componentPlotAllocationOperationDepGraph);
				allGraphProducers.add(componentPlotAllocationOperationDepGraph);
			}

			OperationDependencyGraphAssemblyFilter componentPlotAssemblyOperationDepGraph = null;
			if (retVal && !this.settings.getPlotAssemblyOperationDependencyGraph().isEmpty()) {
				numRequestedTasks++;
				final Configuration configuration = new Configuration();
				componentPlotAssemblyOperationDepGraph = new OperationDependencyGraphAssemblyFilter(configuration,
						this.analysisController);

				final String[] nodeDecorations = Convert.listToArray(this.settings.getPlotAssemblyOperationDependencyGraph());
				this.addDecorators(nodeDecorations, componentPlotAssemblyOperationDepGraph);

				this.analysisController.connect(mtReconstrFilter,
						TraceReconstructionFilter.OUTPUT_PORT_NAME_MESSAGE_TRACE,
						componentPlotAssemblyOperationDepGraph,
						AbstractMessageTraceProcessingFilter.INPUT_PORT_NAME_MESSAGE_TRACES);
				this.analysisController.connect(traceEvents2ExecutionAndMessageTraceFilter,
						TraceEventRecords2ExecutionAndMessageTraceFilter.OUTPUT_PORT_NAME_MESSAGE_TRACE,
						componentPlotAssemblyOperationDepGraph,
						AbstractMessageTraceProcessingFilter.INPUT_PORT_NAME_MESSAGE_TRACES);
				this.analysisController.connect(componentPlotAssemblyOperationDepGraph,
						AbstractTraceAnalysisFilter.REPOSITORY_PORT_NAME_SYSTEM_MODEL, systemEntityFactory);
				allTraceProcessingComponents.add(componentPlotAssemblyOperationDepGraph);
				allGraphProducers.add(componentPlotAssemblyOperationDepGraph);
			}

			TraceCallTreeFilter componentPlotTraceCallTrees = null;
			if (retVal && this.settings.isPlotCallTrees()) {
				numRequestedTasks++;

				componentPlotTraceCallTrees = this.createTraceCallTreeFilter(pathPrefix, systemEntityFactory, mtReconstrFilter,
						traceEvents2ExecutionAndMessageTraceFilter);

				allTraceProcessingComponents.add(componentPlotTraceCallTrees);
			}
			AggregatedAllocationComponentOperationCallTreeFilter componentPlotAggregatedCallTree = null;
			if (retVal && this.settings.isPlotAggregatedDeploymentCallTree()) {
				numRequestedTasks++;
				final Configuration componentPlotAggregatedCallTreeConfig = new Configuration();
				componentPlotAggregatedCallTreeConfig.setProperty(AbstractAnalysisComponent.CONFIG_NAME,
						Constants.PLOTAGGREGATEDALLOCATIONCALLTREE_COMPONENT_NAME);
				componentPlotAggregatedCallTreeConfig.setProperty(
						AbstractAggregatedCallTreeFilter.CONFIG_PROPERTY_NAME_INCLUDE_WEIGHTS, Boolean.toString(true));
				componentPlotAggregatedCallTreeConfig.setProperty(
						AbstractAggregatedCallTreeFilter.CONFIG_PROPERTY_NAME_SHORT_LABELS,
						this.booleanToString(this.settings.isShortLabels()));
				componentPlotAggregatedCallTreeConfig.setProperty(
						AbstractAggregatedCallTreeFilter.CONFIG_PROPERTY_NAME_OUTPUT_FILENAME,
						pathPrefix + Constants.AGGREGATED_ALLOCATION_CALL_TREE_FN_PREFIX + ".dot");
				componentPlotAggregatedCallTree = new AggregatedAllocationComponentOperationCallTreeFilter(
						componentPlotAggregatedCallTreeConfig, this.analysisController);

				this.analysisController.connect(mtReconstrFilter,
						TraceReconstructionFilter.OUTPUT_PORT_NAME_MESSAGE_TRACE, componentPlotAggregatedCallTree,
						AbstractMessageTraceProcessingFilter.INPUT_PORT_NAME_MESSAGE_TRACES);
				this.analysisController.connect(traceEvents2ExecutionAndMessageTraceFilter,
						TraceEventRecords2ExecutionAndMessageTraceFilter.OUTPUT_PORT_NAME_MESSAGE_TRACE,
						componentPlotAggregatedCallTree,
						AbstractMessageTraceProcessingFilter.INPUT_PORT_NAME_MESSAGE_TRACES);
				this.analysisController.connect(componentPlotAggregatedCallTree,
						AbstractTraceAnalysisFilter.REPOSITORY_PORT_NAME_SYSTEM_MODEL, systemEntityFactory);
				allTraceProcessingComponents.add(componentPlotAggregatedCallTree);
			}

			AggregatedAssemblyComponentOperationCallTreeFilter componentPlotAssemblyCallTree = null;
			if (retVal && this.settings.isPlotAggregatedAssemblyCallTree()) {
				numRequestedTasks++;

				componentPlotAssemblyCallTree = this.createAggrAssemblyCompOpCallTreeFilter(pathPrefix, systemEntityFactory,
						mtReconstrFilter, traceEvents2ExecutionAndMessageTraceFilter);

				allTraceProcessingComponents.add(componentPlotAssemblyCallTree);
			}
			if (retVal && this.settings.isPrintDeploymentEquivalenceClasses()) {
				numRequestedTasks++;
				// the actual execution of the task is performed below
			}
			if (this.settings.isPrintSystemModel()) {
				numRequestedTasks++;
			}

			// Attach graph processors to the graph producers
			this.attachGraphProcessors(pathPrefix, allGraphProducers);

			if (numRequestedTasks == 0) {
				this.logger.error("No task requested");
				this.logger.info("Use the option `--help` for usage information");
				return false;
			}

			if (retVal) {
				final String systemEntitiesHtmlFn = pathPrefix + "system-entities.html";
				final Configuration systemModel2FileFilterConfig = new Configuration();
				systemModel2FileFilterConfig.setProperty(SystemModel2FileFilter.CONFIG_PROPERTY_NAME_HTML_OUTPUT_FN,
						systemEntitiesHtmlFn);
				final SystemModel2FileFilter systemModel2FileFilter = new SystemModel2FileFilter(
						systemModel2FileFilterConfig, this.analysisController);
				// note that this plugin is (currently) not connected to any other filters
				this.analysisController.connect(systemModel2FileFilter,
						AbstractTraceAnalysisFilter.REPOSITORY_PORT_NAME_SYSTEM_MODEL, systemEntityFactory);
			}

			int numErrorCount = 0;
			try {
				this.analysisController.run();
				if (this.analysisController.getState() != AnalysisController.STATE.TERMINATED) {
					// Analysis did not terminate successfully
					retVal = false; // Error message referring to log will be printed later
					this.logger.error("Analysis instance terminated in state other than {}: {}", AnalysisController.STATE.TERMINATED,
							this.analysisController.getState());
				}
			} finally {
				for (final AbstractTraceProcessingFilter c : allTraceProcessingComponents) {
					numErrorCount += c.getErrorCount();
					c.printStatusMessage();
				}
				final String kaxOutputFn = pathPrefix + "traceAnalysis.kax";
				final File kaxOutputFile = new File(kaxOutputFn);
				try { // NOCS (nested try)
						// Try to serialize analysis configuration to .kax file
					this.analysisController.saveToFile(kaxOutputFile);
					this.logger.info("Saved analysis configuration to file '{}'", kaxOutputFile.getCanonicalPath());
				} catch (final IOException ex) {
					this.logger.error("Failed to save analysis configuration to file '{}'", kaxOutputFile.getCanonicalPath());
				}
			}
			if (!this.settings.isIgnoreInvalidTraces() && (numErrorCount > 0)) {
				throw new Exception(numErrorCount + " errors occured in trace processing components");
			}

			if (retVal && this.settings.isPrintDeploymentEquivalenceClasses()) {
				retVal = this.writeTraceEquivalenceReport(
						pathPrefix + Constants.TRACE_ALLOCATION_EQUIV_CLASSES_FN_PREFIX + ".txt",
						traceAllocationEquivClassFilter);
			}

			if (retVal && this.settings.isPrintAssemblyEquivalenceClasses()) {
				retVal = this.writeTraceEquivalenceReport(
						pathPrefix + Constants.TRACE_ASSEMBLY_EQUIV_CLASSES_FN_PREFIX + ".txt",
						traceAssemblyEquivClassFilter);
			}
		} catch (final Exception ex) { // NOPMD NOCS (IllegalCatchCheck)
			this.logger.error("An error occured", ex);
			retVal = false;
		} finally {
			if (numRequestedTasks > 0) {
				if (mtReconstrFilter != null) {
					mtReconstrFilter.printStatusMessage();
				}
				if (eventRecordTraceCounter != null) {
					eventRecordTraceCounter.printStatusMessage();
				}
				if (traceEvents2ExecutionAndMessageTraceFilter != null) {
					traceEvents2ExecutionAndMessageTraceFilter.printStatusMessage();
				}
			}
		}

		return retVal;
	}

	private String booleanToString(final Boolean value) {
		if (value == null) {
			return "false";
		} else {
			return Boolean.toString(value);
		}
	}

	private String longToString(final Long value) {
		if (value == null) {
			return "0";
		} else {
			return Long.toString(value);
		}
	}

	/**
	 *
	 * @param decoratorNames
	 * @param plugin
	 */
	private void addDecorators(final String[] decoratorNames, final AbstractDependencyGraphFilter<?> plugin) {
		if (decoratorNames == null) {
			return;
		}
		final List<String> decoratorList = Arrays.asList(decoratorNames);
		final Iterator<String> decoratorIterator = decoratorList.iterator();

		while (decoratorIterator.hasNext()) {
			final String currentDecoratorStr = decoratorIterator.next();
			if (Constants.RESPONSE_TIME_DECORATOR_FLAG_NS.equals(currentDecoratorStr)) {
				plugin.addDecorator(new ResponseTimeNodeDecorator(TimeUnit.NANOSECONDS));
				continue;
			} else if (Constants.RESPONSE_TIME_DECORATOR_FLAG_US.equals(currentDecoratorStr)) {
				plugin.addDecorator(new ResponseTimeNodeDecorator(TimeUnit.MICROSECONDS));
				continue;
			} else if (Constants.RESPONSE_TIME_DECORATOR_FLAG_MS.equals(currentDecoratorStr)) {
				plugin.addDecorator(new ResponseTimeNodeDecorator(TimeUnit.MILLISECONDS));
				continue;
			} else if (Constants.RESPONSE_TIME_DECORATOR_FLAG_S.equals(currentDecoratorStr)) {
				plugin.addDecorator(new ResponseTimeNodeDecorator(TimeUnit.SECONDS));
				continue;
			} else if (Constants.RESPONSE_TIME_COLORING_DECORATOR_FLAG.equals(currentDecoratorStr)) {
				// if decorator is responseColoring, next value should be the threshold
				final String thresholdStringStr = decoratorIterator.next();

				try {
					final int threshold = Integer.parseInt(thresholdStringStr);

					plugin.addDecorator(new ResponseTimeColorNodeDecorator(threshold));
				} catch (final NumberFormatException exc) {
					this.logger.error("Failed to parse int value of property " + "threshold(ms) : " + thresholdStringStr);
				}
			} else {
				this.logger.warn("Unknown decoration name '{}'.", currentDecoratorStr);
				return;
			}
		}
	}

	/**
	 * @param pathPrefix
	 *            prefix path for all files
	 * @param systemEntityFactory
	 *            the consumer filter
	 * @param mtReconstrFilter
	 *            one of the producer filters
	 * @param traceEvents2ExecutionAndMessageTraceFilter
	 *            one of the producer filters
	 * @return
	 * @throws IOException
	 * @throws AnalysisConfigurationException
	 */
	private TraceCallTreeFilter createTraceCallTreeFilter(final String pathPrefix, final SystemModelRepository systemEntityFactory,
			final TraceReconstructionFilter mtReconstrFilter,
			final TraceEventRecords2ExecutionAndMessageTraceFilter traceEvents2ExecutionAndMessageTraceFilter)
			throws IOException, AnalysisConfigurationException {
		// build config
		final Configuration componentPlotTraceCallTreesConfig = new Configuration();

		componentPlotTraceCallTreesConfig.setProperty(TraceCallTreeFilter.CONFIG_PROPERTY_NAME_OUTPUT_FILENAME,
				new File(pathPrefix + Constants.CALL_TREE_FN_PREFIX)
						.getCanonicalPath());
		componentPlotTraceCallTreesConfig.setProperty(TraceCallTreeFilter.CONFIG_PROPERTY_NAME_SHORT_LABELS,
				this.booleanToString(this.settings.isShortLabels()));
		componentPlotTraceCallTreesConfig.setProperty(AbstractAnalysisComponent.CONFIG_NAME,
				Constants.PLOTCALLTREE_COMPONENT_NAME);

		// create filter
		final TraceCallTreeFilter componentPlotTraceCallTrees = new TraceCallTreeFilter(componentPlotTraceCallTreesConfig,
				this.analysisController);

		// connect filter
		this.analysisController.connect(mtReconstrFilter, TraceReconstructionFilter.OUTPUT_PORT_NAME_MESSAGE_TRACE,
				componentPlotTraceCallTrees, AbstractMessageTraceProcessingFilter.INPUT_PORT_NAME_MESSAGE_TRACES);
		this.analysisController.connect(traceEvents2ExecutionAndMessageTraceFilter,
				TraceEventRecords2ExecutionAndMessageTraceFilter.OUTPUT_PORT_NAME_MESSAGE_TRACE,
				componentPlotTraceCallTrees, AbstractMessageTraceProcessingFilter.INPUT_PORT_NAME_MESSAGE_TRACES);
		this.analysisController.connect(componentPlotTraceCallTrees,
				AbstractTraceAnalysisFilter.REPOSITORY_PORT_NAME_SYSTEM_MODEL, systemEntityFactory);

		return componentPlotTraceCallTrees;
	}

	/**
	 * @param pathPrefix
	 * @param systemEntityFactory
	 *            the consumer filter
	 * @param traceReconstructionFilter
	 *            one of the producer filters
	 * @param traceEvents2ExecutionAndMessageTraceFilter
	 *            one of the producer filters
	 * @return
	 * @throws AnalysisConfigurationException
	 */
	private AggregatedAssemblyComponentOperationCallTreeFilter createAggrAssemblyCompOpCallTreeFilter(
			final String pathPrefix,
			final SystemModelRepository systemEntityFactory, final TraceReconstructionFilter traceReconstructionFilter,
			final TraceEventRecords2ExecutionAndMessageTraceFilter traceEvents2ExecutionAndMessageTraceFilter)
			throws AnalysisConfigurationException {
		// build configuration
		final Configuration componentPlotAssemblyCallTreeConfig = new Configuration();
		componentPlotAssemblyCallTreeConfig.setProperty(AbstractAnalysisComponent.CONFIG_NAME,
				Constants.PLOTAGGREGATEDASSEMBLYCALLTREE_COMPONENT_NAME);
		componentPlotAssemblyCallTreeConfig.setProperty(
				AbstractAggregatedCallTreeFilter.CONFIG_PROPERTY_NAME_INCLUDE_WEIGHTS, Boolean.toString(true));
		componentPlotAssemblyCallTreeConfig.setProperty(
				AbstractAggregatedCallTreeFilter.CONFIG_PROPERTY_NAME_SHORT_LABELS, this.booleanToString(this.settings.isShortLabels()));
		componentPlotAssemblyCallTreeConfig.setProperty(
				AbstractAggregatedCallTreeFilter.CONFIG_PROPERTY_NAME_OUTPUT_FILENAME, pathPrefix + Constants.AGGREGATED_ASSEMBLY_CALL_TREE_FN_PREFIX + ".dot");

		// create filter
		final AggregatedAssemblyComponentOperationCallTreeFilter componentPlotAssemblyCallTree = new AggregatedAssemblyComponentOperationCallTreeFilter(
				componentPlotAssemblyCallTreeConfig, this.analysisController);

		// connect filter
		// the input port of this filter is connected with two source filters
		final String inputPort = AbstractMessageTraceProcessingFilter.INPUT_PORT_NAME_MESSAGE_TRACES;

		this.analysisController.connect(traceReconstructionFilter,
				TraceReconstructionFilter.OUTPUT_PORT_NAME_MESSAGE_TRACE, componentPlotAssemblyCallTree, inputPort);
		this.analysisController.connect(traceEvents2ExecutionAndMessageTraceFilter,
				TraceEventRecords2ExecutionAndMessageTraceFilter.OUTPUT_PORT_NAME_MESSAGE_TRACE,
				componentPlotAssemblyCallTree, inputPort);
		this.analysisController.connect(componentPlotAssemblyCallTree,
				AbstractTraceAnalysisFilter.REPOSITORY_PORT_NAME_SYSTEM_MODEL, systemEntityFactory);

		return componentPlotAssemblyCallTree;
	}

	/**
	 * Attaches graph processors and a writer to the given graph producers depending on the given command line.
	 *
	 * @param graphProducers
	 *            The graph producers to connect processors to
	 * @param controller
	 *            The analysis controller to use for the connection of the plugins
	 * @param commandLine
	 *            The command line to determine the desired processors
	 *
	 * @throws IllegalStateException
	 *             If the connection of plugins is not possible at the moment
	 * @throws AnalysisConfigurationException
	 *             If some plugins cannot be connected
	 */
	private void attachGraphProcessors(final String pathPrefix, final List<AbstractGraphProducingFilter<?>> graphProducers)
			throws IllegalStateException, AnalysisConfigurationException, IOException {

		for (final AbstractGraphProducingFilter<?> producer : graphProducers) {
			AbstractGraphFilter<?, ?, ?, ?> lastFilter = null;

			// Add a trace coloring filter, if necessary
			if (this.settings.getTraceColoringFile() != null) {
				final String coloringFileName = this.settings.getTraceColoringFile().getAbsolutePath();
				lastFilter = PerformAnalysis.createTraceColoringFilter(producer, coloringFileName, this.analysisController);
			}

			// Add a description filter, if necessary
			if (this.settings.getAddDescriptions() != null) {
				final String descriptionsFileName = this.settings.getAddDescriptions().getAbsolutePath();
				if (lastFilter != null) {
					lastFilter = PerformAnalysis.createDescriptionDecoratorFilter(lastFilter, descriptionsFileName,
							this.analysisController);
				} else {
					lastFilter = PerformAnalysis.createDescriptionDecoratorFilter(producer, descriptionsFileName,
							this.analysisController);
				}
			}

			if (lastFilter != null) {
				this.attachGraphWriter(pathPrefix, lastFilter, producer);
			} else {
				this.attachGraphWriter(pathPrefix, producer, producer);
			}
		}
	}

	/**
	 * Attaches a graph writer plugin to the given plugin.
	 *
	 * @param plugin
	 *            The plugin which delivers the graph to write
	 * @param producer
	 *            The producer which originally produced the graph
	 * @throws IllegalStateException
	 *             If the connection of the plugins is not possible at the moment
	 * @throws AnalysisConfigurationException
	 *             If the plugins cannot be connected
	 *
	 * @param <P>
	 *            The type of the plugin.
	 */
	private <P extends AbstractPlugin & IGraphOutputtingFilter<?>> void attachGraphWriter(final String pathPrefix, final P plugin,
			final AbstractGraphProducingFilter<?> producer)
			throws IllegalStateException, AnalysisConfigurationException {

		final Configuration configuration = new Configuration();
		configuration.setProperty(GraphWriterPlugin.CONFIG_PROPERTY_NAME_OUTPUT_PATH_NAME, pathPrefix);
		configuration.setProperty(GraphWriterPlugin.CONFIG_PROPERTY_NAME_INCLUDE_WEIGHTS, String.valueOf(true));
		configuration.setProperty(GraphWriterPlugin.CONFIG_PROPERTY_NAME_SHORTLABELS, String.valueOf(this.settings.isShortLabels()));
		configuration.setProperty(GraphWriterPlugin.CONFIG_PROPERTY_NAME_SELFLOOPS,
				String.valueOf(this.settings.isIncludeSelfLoops()));
		configuration.setProperty(AbstractAnalysisComponent.CONFIG_NAME, producer.getConfigurationName());
		final GraphWriterPlugin graphWriter = new GraphWriterPlugin(configuration, this.analysisController);
		this.analysisController.connect(plugin, plugin.getGraphOutputPortName(), graphWriter,
				GraphWriterPlugin.INPUT_PORT_NAME_GRAPHS);
	}

	/**
	 *
	 * @param predecessor
	 * @param filter
	 * @param controller
	 * @throws IllegalStateException
	 * @throws AnalysisConfigurationException
	 */
	private static <P extends AbstractPlugin & IGraphOutputtingFilter<?>> void connectGraphFilters(final P predecessor,
			final AbstractGraphFilter<?, ?, ?, ?> filter, final AnalysisController controller)
			throws IllegalStateException, AnalysisConfigurationException {
		controller.connect(predecessor, predecessor.getGraphOutputPortName(), filter, filter.getGraphInputPortName());
	}

	private static <P extends AbstractPlugin & IGraphOutputtingFilter<?>> TraceColoringFilter<?, ?> createTraceColoringFilter(
			final P predecessor, final String coloringFileName, final AnalysisController controller)
			throws IOException, IllegalStateException, AnalysisConfigurationException {
		final TraceColorRepository colorRepository = TraceColorRepository.createFromFile(coloringFileName, controller);

		@SuppressWarnings("rawtypes")
		final TraceColoringFilter<?, ?> coloringFilter = new TraceColoringFilter(new Configuration(), controller);
		PerformAnalysis.connectGraphFilters(predecessor, coloringFilter, controller);
		controller.connect(coloringFilter, TraceColoringFilter.COLOR_REPOSITORY_PORT_NAME, colorRepository);

		return coloringFilter;
	}

	/**
	 *
	 * @param predecessor
	 * @param descriptionsFileName
	 * @param controller
	 * @return
	 * @throws IOException
	 * @throws IllegalStateException
	 * @throws AnalysisConfigurationException
	 */
	private static <P extends AbstractPlugin & IGraphOutputtingFilter<?>> DescriptionDecoratorFilter<?, ?, ?> createDescriptionDecoratorFilter(
			final P predecessor, final String descriptionsFileName, final AnalysisController controller)
			throws IOException, IllegalStateException, AnalysisConfigurationException {
		final DescriptionRepository descriptionRepository = DescriptionRepository.createFromFile(descriptionsFileName,
				controller);

		@SuppressWarnings("rawtypes")
		final DescriptionDecoratorFilter<?, ?, ?> descriptionFilter = new DescriptionDecoratorFilter(
				new Configuration(), controller);
		PerformAnalysis.connectGraphFilters(predecessor, descriptionFilter, controller);
		controller.connect(descriptionFilter, DescriptionDecoratorFilter.DESCRIPTION_REPOSITORY_PORT_NAME,
				descriptionRepository);

		return descriptionFilter;
	}

	/**
	 * Write trace equivalent report.
	 *
	 * @param outputFnPrefixL
	 *            path prefix
	 * @param traceEquivFilter
	 *            filter
	 * @return true on success
	 * @throws IOException
	 *             on error
	 */
	private boolean writeTraceEquivalenceReport(final String outputFnPrefixL,
			final TraceEquivalenceClassFilter traceEquivFilter) throws IOException {
		boolean retVal = true;
		final String outputFn = new File(outputFnPrefixL).getCanonicalPath();
		PrintStream ps = null;
		try {
			ps = new PrintStream(new FileOutputStream(outputFn), false, ENCODING);
			int numClasses = 0;
			final Map<ExecutionTrace, Integer> classMap = traceEquivFilter.getEquivalenceClassMap(); // NOPMD
																										// (UseConcurrentHashMap)
			for (final Entry<ExecutionTrace, Integer> e : classMap.entrySet()) {
				final ExecutionTrace t = e.getKey();
				ps.println("Class " + numClasses++ + " ; cardinality: " + e.getValue() + "; # executions: " + t.getLength() + "; representative: " + t.getTraceId()
						+ "; max. stack depth: " + t.getMaxEss());
			}
			this.logger.debug("");
			this.logger.debug("#");
			this.logger.debug("# Plugin: Trace equivalence report");
			this.logger.debug("Wrote {} equivalence class{} to file '{}'", numClasses, (numClasses > 1 ? "es" : ""), outputFn); // NOCS
		} catch (final FileNotFoundException e) {
			this.logger.error("File not found", e);
			retVal = false;
		} finally {
			if (ps != null) {
				ps.close();
			}
		}

		return retVal;
	}

}