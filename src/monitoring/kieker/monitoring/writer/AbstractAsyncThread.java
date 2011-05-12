package kieker.monitoring.writer;

import java.util.concurrent.BlockingQueue;

import kieker.common.record.DummyMonitoringRecord;
import kieker.common.record.IMonitoringRecord;
import kieker.monitoring.core.controller.IMonitoringController;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Andre van Hoorn, Jan Waller
 */
public abstract class AbstractAsyncThread extends Thread {
	private static final Log log = LogFactory.getLog(AbstractAsyncThread.class);

	private final static IMonitoringRecord END_OF_MONITORING_MARKER = new DummyMonitoringRecord();
	private volatile boolean finished = false;
	private final BlockingQueue<IMonitoringRecord> writeQueue;
	private final IMonitoringController monitoringController;

	public AbstractAsyncThread(final IMonitoringController monitoringController, final BlockingQueue<IMonitoringRecord> writeQueue) {
		this.writeQueue = writeQueue;
		this.monitoringController = monitoringController;
	}

	public final void initShutdown() {
		try {
			this.writeQueue.put(END_OF_MONITORING_MARKER);
		} catch (final InterruptedException ex) {
			AbstractAsyncThread.log.error("Error while trying to stop writer thread", ex);
		}
	}

	public final boolean isFinished() {
		return this.finished;
	}

	@Override
	public final void run() {
		AbstractAsyncThread.log.debug(this.getClass().getName() + " running");
		// making it a local variable for faster access
		final BlockingQueue<IMonitoringRecord> writeQueue = this.writeQueue;
		try {
			while (!this.finished) {
				try {
					IMonitoringRecord monitoringRecord = writeQueue.take();
					if (monitoringRecord == END_OF_MONITORING_MARKER) {
						AbstractAsyncThread.log.debug("Terminating writer thread, " + writeQueue.size() + " entries remaining");
						monitoringRecord = writeQueue.poll();
						while (monitoringRecord != null) {
							if (monitoringRecord != END_OF_MONITORING_MARKER) {
								this.consume(monitoringRecord);
							}
							monitoringRecord = writeQueue.poll();
						}
						this.finished = true;
						this.writeQueue.put(END_OF_MONITORING_MARKER);
						this.cleanup();
						break;
					} else {
						this.consume(monitoringRecord);
					}
				} catch (final InterruptedException ex) {
					continue;
					// would be another method to finish the execution
					// but normally we should be able to continue
				}
			}
			AbstractAsyncThread.log.debug("Writer thread finished");
		} catch (final Exception ex) {
			// e.g. Interrupted Exception or IOException
			AbstractAsyncThread.log.error("Writer thread will halt", ex);
			this.finished = true;
			this.cleanup();
			this.monitoringController.terminateMonitoring();
		} finally {
			this.finished = true;
		}
	}

	/**
	 * Returns a human-readable information string about the writer's configuration and state.
	 * 
	 * @return the information string.
	 */
	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append("Finished: '");
		sb.append(this.isFinished());
		sb.append("'");
		return sb.toString();
	}

	protected abstract void consume(final IMonitoringRecord monitoringRecord) throws Exception;

	protected abstract void cleanup();
}