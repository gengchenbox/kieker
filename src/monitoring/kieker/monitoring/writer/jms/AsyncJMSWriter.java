package kieker.monitoring.writer.jms;

import kieker.common.record.MonitoringRecordTypeClassnameMapping;
import kieker.monitoring.writer.util.async.AbstractWorkerThread;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import kieker.monitoring.core.MonitoringController;
import kieker.common.record.DummyMonitoringRecord;
import kieker.common.record.IMonitoringRecord;
import kieker.common.util.PropertyMap;
import kieker.monitoring.writer.IMonitoringLogWriter;

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
/**
 *
 * @author Matthias Rohr, Andre van Hoorn
 */
public final class AsyncJMSWriter implements IMonitoringLogWriter {

    private static final Log log = LogFactory.getLog(AsyncJMSWriter.class);
    private Vector<AbstractWorkerThread> typeWriterAndRecordWriters = new Vector<AbstractWorkerThread>();
    private static final MonitoringRecordTypeClassnameMapping TYPE_WRITER_END_OF_MONITORING_MARKER = new MonitoringRecordTypeClassnameMapping(-1, null);
    private static final IMonitoringRecord RECORD_WRITER_END_OF_MONITORING_MARKER = new DummyMonitoringRecord();
    private final int numberOfJmsWriters = 3; // number of jms connections -- usually one (on every node)        
    private BlockingQueue<IMonitoringRecord> recordQueue = null;
    private BlockingQueue<MonitoringRecordTypeClassnameMapping> typeQueue = null;
    private String contextFactoryType; // type of the jms factory implementation, e.g.
    private String providerUrl;
    private String factoryLookupName;
    private String topic;
    private long messageTimeToLive;
    private int asyncRecordQueueSize = 8000;
    private int asyncTypeQueueSize = 20;

    /**
     * Init String. Expect key=value pairs separated by |.
     * 
     * Example initString (meaning of keys explained below):
     * jmsProviderUrl=tcp://localhost:3035/ | jmsTopic=queue1 | jmsContextFactoryType=org.exolab.jms.jndi.InitialContextFactory | jmsFactoryLookupName=ConnectionFactory | jmsMessageTimeToLive = 10000
     * 
     * jmsContextFactoryType -- type of the jms factory implementation, e.g. "org.exolab.jms.jndi.InitialContextFactory" for openjms 0.7.7
     * jmsProviderUrl -- url of the jndi provider that knows the jms service
     * jmsFactoryLookupName -- service name for the jms connection factory
     * jmsTopic -- topic at the jms server which is used in the publisher/subscribe communication
     * jmsMessageTimeToLive -- time that a jms message will kepts be alive at the jms server before it is automatically deleted
     * 
     * @param initString
     * @return true on success. false on error.
     */
    public boolean init(String initString) {
        if (initString == null || initString.length() == 0) {
            log.error("Invalid initString. Valid example for kieker.monitoring.properties:\n"
                    + "monitoringDataWriterInitString=jmsProviderUrl=tcp://localhost:3035/ | jmsTopic=queue1 | jmsContextFactoryType=org.exolab.jms.jndi.InitialContextFactory | jmsFactoryLookupName=ConnectionFactory | jmsMessageTimeToLive = 10000");
            return false;
        }

        boolean retVal = true;
        try {
            final PropertyMap propertyMap = new PropertyMap(initString, "|", "=");
            
            this.contextFactoryType = propertyMap.getProperty("jmsContextFactoryType");
            this.providerUrl = propertyMap.getProperty("jmsProviderUrl");
            this.factoryLookupName = propertyMap.getProperty("jmsFactoryLookupName");
            this.topic = propertyMap.getProperty("jmsTopic");
            this.messageTimeToLive = Long.valueOf(propertyMap.getProperty("jmsMessageTimeToLive"));
            this.asyncRecordQueueSize = Integer.valueOf(propertyMap.getProperty("asyncRecordQueueSize"));


            this.recordQueue = new ArrayBlockingQueue<IMonitoringRecord>(asyncRecordQueueSize);
            this.typeQueue = new ArrayBlockingQueue<MonitoringRecordTypeClassnameMapping>(asyncTypeQueueSize);
            for (int i = 1; i <= numberOfJmsWriters; i++) {
                JMSWriterThread<IMonitoringRecord> recordWriter =
                        new JMSWriterThread<IMonitoringRecord>(recordQueue, AsyncJMSWriter.RECORD_WRITER_END_OF_MONITORING_MARKER, contextFactoryType, providerUrl, factoryLookupName, topic, messageTimeToLive);
                typeWriterAndRecordWriters.add(recordWriter);
                recordWriter.setDaemon(true);
                recordWriter.start();
            }
            log.info("(" + numberOfJmsWriters + " threads) will send to the JMS server topic");
        } catch (Exception exc) {
            log.fatal("Error initiliazing JMS Connector", exc);
            retVal = false;
        }
        return retVal;
    }

    
    public String getInfoString() {
        StringBuilder strB = new StringBuilder();

        strB.append("contextFactoryType : " + this.contextFactoryType);
        strB.append("providerUrl : " + this.providerUrl);
        strB.append("factoryLookupName : " + this.factoryLookupName);
        strB.append("topic : " + this.topic);
        strB.append("messageTimeToLive : " + this.messageTimeToLive);

        return strB.toString();
    }

    
    public boolean newMonitoringRecord(IMonitoringRecord monitoringRecord) {
        try {
            if (monitoringRecord == MonitoringController.END_OF_MONITORING_MARKER) {
                // "translate" END_OF_MONITORING_MARKER
                log.info("Found END_OF_MONITORING_MARKER. Notifying type and record writers");
                this.typeQueue.add(AsyncJMSWriter.TYPE_WRITER_END_OF_MONITORING_MARKER);
                this.recordQueue.add(AsyncJMSWriter.RECORD_WRITER_END_OF_MONITORING_MARKER);
            } else {
                recordQueue.add(monitoringRecord); // tries to add immediately! -- this is for production systems
            }
            //int currentQueueSize = recordQueue.size();
        } catch (Exception ex) {
            log.error(System.currentTimeMillis() + " AsyncJmsProducer() failed: Exception:", ex);
            return false;
        }
        return true;
    }

    
    public Vector<AbstractWorkerThread> getWorkers() {
        return this.typeWriterAndRecordWriters;
    }
}