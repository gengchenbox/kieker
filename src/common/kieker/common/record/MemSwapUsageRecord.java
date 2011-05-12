/**
 * 
 */
package kieker.common.record;


/**
 * @author Andre van Hoorn
 * 
 */
public class MemSwapUsageRecord extends AbstractMonitoringRecord {

	private static final String DEFAULT_VALUE = "N/A";

	public static final double UNDEFINED_DOUBLE = -1;
	public static final long UNDEFINED_LONG = -1;

	/**
	 * Date/time of measurement. The value should be interpreted as the number
	 * of nano-seconds elapsed since Jan 1st, 1970 UTC.
	 */
	private volatile long timestamp = -1;

	/**
	 * Name of the host, the resource belongs to.
	 */
	private volatile String hostName = MemSwapUsageRecord.DEFAULT_VALUE;

	private volatile long memTotal = MemSwapUsageRecord.UNDEFINED_LONG;
	/**
	 * @return the memTotal
	 */
	public final long getMemTotal() {
		return this.memTotal;
	}

	/**
	 * @param memTotal the memTotal to set
	 */
	public final void setMemTotal(final long memTotal) {
		this.memTotal = memTotal;
	}

	/**
	 * @return the memUsed
	 */
	public final long getMemUsed() {
		return this.memUsed;
	}

	/**
	 * @param memUsed the memUsed to set
	 */
	public final void setMemUsed(final long memUsed) {
		this.memUsed = memUsed;
	}

	/**
	 * @return the memFree
	 */
	public final long getMemFree() {
		return this.memFree;
	}

	/**
	 * @param memFree the memFree to set
	 */
	public final void setMemFree(final long memFree) {
		this.memFree = memFree;
	}

	/**
	 * @return the swapTotal
	 */
	public final long getSwapTotal() {
		return this.swapTotal;
	}

	/**
	 * @param swapTotal the swapTotal to set
	 */
	public final void setSwapTotal(final long swapTotal) {
		this.swapTotal = swapTotal;
	}

	/**
	 * @return the swapUsed
	 */
	public final long getSwapUsed() {
		return this.swapUsed;
	}

	/**
	 * @param swapUsed the swapUsed to set
	 */
	public final void setSwapUsed(final long swapUsed) {
		this.swapUsed = swapUsed;
	}

	/**
	 * @return the swapFree
	 */
	public final long getSwapFree() {
		return this.swapFree;
	}

	/**
	 * @param swapFree the swapFree to set
	 */
	public final void setSwapFree(final long swapFree) {
		this.swapFree = swapFree;
	}

	private volatile long memUsed = MemSwapUsageRecord.UNDEFINED_LONG;
	private volatile long memFree = MemSwapUsageRecord.UNDEFINED_LONG;
	private volatile long swapTotal = MemSwapUsageRecord.UNDEFINED_LONG;
	private volatile long swapUsed = MemSwapUsageRecord.UNDEFINED_LONG;
	private volatile long swapFree = MemSwapUsageRecord.UNDEFINED_LONG;

	private static final long serialVersionUID = 1762476L;

	/*
	 * {@inheritdoc}
	 */
	@Override
	public void initFromArray(final Object[] values)
			throws IllegalArgumentException {
		try {
			if (values.length != MemSwapUsageRecord.VALUE_TYPES.length) {
				throw new IllegalArgumentException("Expecting vector with "
						+ MemSwapUsageRecord.VALUE_TYPES.length
						+ " elements but found:" + values.length);
			}

			this.timestamp = (Long) values[0];
			this.hostName = (String) values[1];
			this.memTotal = (Long) values[2];
			this.memUsed = (Long) values[3];
			this.memFree = (Long) values[4];
			this.swapTotal = (Long) values[5];
			this.swapUsed = (Long) values[6];
			this.swapFree = (Long) values[7];

		} catch (final Exception exc) {
			throw new IllegalArgumentException("Failed to init", exc);
		}
		return;
	}

	/**
	 * 
	 */
	public MemSwapUsageRecord() {
	}

	/**
	 * Constructs a new {@link MemSwapUsageRecord} with the given values. If
	 * certain values shall remain undefined, use the constants
	 * {@link #UNDEFINED_DOUBLE} and {@link #UNDEFINED_LONG}.
	 * 
	 */
	public MemSwapUsageRecord(final long timestamp, final String hostName,
			final long memTotal, final long memUsed, final long memFree,
			final long swapTotal, final long swapUsed, final long swapFree) {
		this.timestamp = timestamp;
		this.hostName = hostName;
		this.memTotal = memTotal;
		this.memUsed = memUsed;
		this.memFree = memFree;
		this.swapTotal = swapTotal;
		this.swapUsed = swapUsed;
		this.swapFree = swapFree;
	}

	/*
	 * {@inheritdoc}
	 */
	@Override
	public Object[] toArray() {
		return new Object[] { this.timestamp, this.hostName, this.memTotal,
				this.memUsed, this.memFree, this.swapTotal, this.swapUsed,
				this.swapFree };
	}

	private final static Class<?>[] VALUE_TYPES = { long.class, String.class,
			long.class, long.class, long.class, long.class, long.class,
			long.class };

	/*
	 * {@inheritdoc}
	 */
	@Override
	public Class<?>[] getValueTypes() {
		return MemSwapUsageRecord.VALUE_TYPES;
	}

	/**
	 * @return the timestamp
	 */
	public final long getTimestamp() {
		return this.timestamp;
	}

	/**
	 * @param timestamp
	 *            the timestamp to set
	 */
	public final void setTimestamp(final long timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * @return the hostName
	 */
	public final String getHostName() {
		return this.hostName;
	}

	/**
	 * @param hostName
	 *            the hostName to set
	 */
	public final void setHostName(final String hostName) {
		this.hostName = hostName;
	}
}