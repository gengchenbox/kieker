package kieker.test.tools.junit.traceAnalysis.plugins;

/*
 * ==================LICENCE=========================
 * Copyright 2006-2010 Kieker Project
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

import junit.framework.TestCase;
import kieker.analysis.plugin.configuration.AbstractInputPort;
import kieker.test.tools.junit.traceAnalysis.util.ExecutionFactory;
import kieker.tools.traceAnalysis.plugins.traceFilter.TraceEquivalenceClassFilter;
import kieker.tools.traceAnalysis.plugins.traceReconstruction.InvalidTraceException;
import kieker.tools.traceAnalysis.systemModel.Execution;
import kieker.tools.traceAnalysis.systemModel.ExecutionTrace;
import kieker.tools.traceAnalysis.systemModel.repository.SystemModelRepository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author Andre van Hoorn
 */
public class TestTraceEquivalenceFilterAssemblyEquivalence extends TestCase {

    private static final Log log = LogFactory.getLog(TestTraceReconstructionFilter.class);
    private final SystemModelRepository systemEntityFactory = new SystemModelRepository();
    private final ExecutionFactory executionFactory = new ExecutionFactory(systemEntityFactory);
    
    public void testEqualTrace(){
        final ExecutionTrace trace0, trace1;

        try {
            trace0 = genValidBookstoreTrace(45653l, 17);
            trace1 = genValidBookstoreTrace(45653l, 17);
        } catch (InvalidTraceException ex) {
            log.error("InvalidTraceException", ex);
            fail("InvalidTraceException" + ex);
            return;
        }
        assertEquals(trace0, trace1);

        TraceEquivalenceClassFilter filter = new TraceEquivalenceClassFilter(
                "TraceEquivalenceClassFilter",
                this.systemEntityFactory,
                TraceEquivalenceClassFilter.TraceEquivalenceClassModes.ASSEMBLY);

        /*
         * Register a handler for equivalence class representatives.
         */
        filter.getExecutionTraceOutputPort().subscribe(new AbstractInputPort<ExecutionTrace>("Execution traces") {

            public void newEvent(ExecutionTrace event) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        });
    }

    private ExecutionTrace genValidBookstoreTrace(final long traceId, final long offset) throws InvalidTraceException {
        /* Executions of a valid trace */
        final Execution exec0_0__bookstore_searchBook;
        final Execution exec1_1__catalog_getBook;
        final Execution exec2_1__crm_getOrders;
        final Execution exec3_2__catalog_getBook;

        /* Manually create Executions for a trace */
        exec0_0__bookstore_searchBook = executionFactory.genExecution(
                "Bookstore", "bookstore", "searchBook",
                traceId,
                1 * (1000 * 1000) + offset, // tin
                10 * (1000 * 1000) + offset, // tout
                0, 0);  // eoi, ess

        exec1_1__catalog_getBook = executionFactory.genExecution(
                "Catalog", "catalog", "getBook",
                traceId,
                2 * (1000 * 1000) + offset, // tin
                4 * (1000 * 1000) + offset, // tout
                1, 1);  // eoi, ess
        exec2_1__crm_getOrders = executionFactory.genExecution(
                "CRM", "crm", "getOrders",
                traceId,
                5 * (1000 * 1000) + offset, // tin
                8 * (1000 * 1000) + offset, // tout
                2, 1);  // eoi, ess
        exec3_2__catalog_getBook = executionFactory.genExecution(
                "Catalog", "catalog", "getBook",
                traceId,
                6 * (1000 * 1000) + offset, // tin
                7 * (1000 * 1000) + offset, // tout
                3, 2);  // eoi, ess

        /*
         * Create an Execution Trace and add Executions in
         * arbitrary order
         */
        ExecutionTrace executionTrace = new ExecutionTrace(traceId);

        executionTrace.add(exec3_2__catalog_getBook);
        executionTrace.add(exec2_1__crm_getOrders);
        executionTrace.add(exec0_0__bookstore_searchBook);
        executionTrace.add(exec1_1__catalog_getBook);

        try {
            /* Make sure that trace is valid: */
            executionTrace.toMessageTrace(this.systemEntityFactory.getRootExecution());
        } catch (InvalidTraceException ex) {
            log.error(ex);
            fail("Test invalid since used trace invalid");
            throw new InvalidTraceException("Test invalid since used trace invalid", ex);
        }

        return executionTrace;
    }
}