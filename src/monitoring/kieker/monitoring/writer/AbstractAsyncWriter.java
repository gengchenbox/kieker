package kieker.monitoring.writer;

import java.util.Properties;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import kieker.common.record.IMonitoringRecord;
import kieker.monitoring.core.configuration.Configuration;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Jan Waller
 */
public abstract class AbstractAsyncWriter extends AbstractMonitoringWriter {
	private static final Log log = LogFactory.getLog(AbstractAsyncWriter.class);

	private static final String QUEUESIZE = "QueueSize";
	private static final String BEHAVIOR = "QueueFullBehavior";

	// internal variables
	private final String PREFIX;
	private final Vector<AbstractAsyncThread> workers = new Vector<AbstractAsyncThread>();
	protected final BlockingQueue<IMonitoringRecord> blockingQueue;
	private final int queueFullBehavior;

	protected AbstractAsyncWriter(final Configuration configuration) {
		super(configuration);
		this.PREFIX = this.getClass().getName() + ".";

		final int queueFullBehavior = this.configuration.getIntProperty(this.PREFIX + BEHAVIOR);
		if ((queueFullBehavior < 0) || (queueFullBehavior > 2)) {
			AbstractAsyncWriter.log.warn("Unknown value '" + queueFullBehavior + "' for " + this.PREFIX + BEHAVIOR + "; using default value 0");
			this.queueFullBehavior = 0;
		} else {
			this.queueFullBehavior = queueFullBehavior;
		}
		this.blockingQueue = new ArrayBlockingQueue<IMonitoringRecord>(this.configuration.getIntProperty(this.PREFIX + QUEUESIZE));
	}

	/**
	 * Make sure that the two required properties always have default values!
	 */
	@Override
	protected Properties getDefaultProperties() {
		final Properties properties = new Properties(super.getDefaultProperties());
		final String PREFIX = this.getClass().getName() + ".";
		properties.setProperty(PREFIX + QUEUESIZE, "10000");
		properties.setProperty(PREFIX + BEHAVIOR, "0");
		return properties;
	}

	/**
	 * This method must be called at the end of the child constructor!
	 * 
	 * @param worker
	 */
	protected final void addWorker(final AbstractAsyncThread worker) {
		this.workers.add(worker);
		worker.setDaemon(true); // might lead to inconsistent data due to harsh shutdown
		worker.start();
	}

	@Override
	public final void terminate() {
		// notify all workers
		for (final AbstractAsyncThread worker : this.workers) {
			worker.initShutdown();
		}
		// wait for all worker to finish
		for (final AbstractAsyncThread worker : this.workers) {
			while (!worker.isFinished()) {
				try {
					Thread.sleep(500);
				} catch (final InterruptedException ex) {
					// we should be able to ignore an interrupted wait
				}
				AbstractAsyncWriter.log.info("shutdown delayed - Worker is busy ... waiting additional 0.5 seconds");
				// TODO: we should be able to abort this, perhaps a max time of repeats?
			}
		}
		AbstractAsyncWriter.log.info("Writer shutdown complete");
	}

	@Override
	public final boolean newMonitoringRecord(final IMonitoringRecord monitoringRecord) {
		try {
			switch (this.queueFullBehavior) {
				case 1: // blocks when queue full
					this.blockingQueue.put(monitoringRecord);
					break;
				case 2: // does nothing if queue is full
					this.blockingQueue.offer(monitoringRecord);
					break;
				default: // tries to add immediately (error if full)
					this.blockingQueue.add(monitoringRecord);
					break;
			}
		} catch (final Exception ex) {
			AbstractAsyncWriter.log.error("Failed to retrieve new monitoring record." + ex);
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		sb.append("\n\tWriter Threads (");
		sb.append(this.workers.size());
		sb.append("): ");
		for (final AbstractAsyncThread worker : this.workers) {
			sb.append("\n\t\t");
			sb.append(worker.toString());
		}
		return sb.toString();
	}
}