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

package kieker.monitoring.sampler.oshi.samplers;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import kieker.common.record.system.NetworkUtilizationRecord;
import kieker.monitoring.core.controller.IMonitoringController;
import kieker.monitoring.core.signaturePattern.SignatureFactory;
import kieker.monitoring.timer.ITimeSource;

import oshi.hardware.HardwareAbstractionLayer;
import oshi.hardware.NetworkIF;

/**
 * Logs network utilization of the system, retrieved as
 * {@link NetworkUtilizationRecord} via
 * {@link kieker.monitoring.core.controller.IMonitoringController#newMonitoringRecord(kieker.common.record.IMonitoringRecord)}
 * .
 *
 * @author Matteo Sassano
 * @since 1.14
 *
 */
public final class NetworkUtilizationSampler extends AbstractOshiSampler {

	private final ConcurrentHashMap<String, NetworkStatistic> networkStatisticMap = new ConcurrentHashMap<>();

	/**
	 * Constructs a new {@link AbstractOshiSampler} with given
	 * {@link HardwareAbstractionLayer} instance used to retrieve the sensor data.
	 * Users should use the factory method
	 * {@link kieker.monitoring.sampler.oshi.OshiSamplerFactory#createSensorNetworkUtilization()}
	 * to acquire an instance rather than calling this constructor directly.
	 *
	 * @param hardwareAbstractionLayer
	 *            The {@link HardwareAbstractionLayer} which will be used to
	 *            retrieve the data.
	 */
	public NetworkUtilizationSampler(final HardwareAbstractionLayer hardwareAbstractionLayer) {
		super(hardwareAbstractionLayer);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void sample(final IMonitoringController monitoringController) {
		if (!monitoringController.isMonitoringEnabled() || !monitoringController.isProbeActivated(SignatureFactory.createNetworkUtilizationSignature())) {
			return;
		}
		final NetworkIF[] networkInterfaces = this.hardwareAbstractionLayer.getNetworkIFs();
		for (final NetworkIF networkIF : networkInterfaces) {

			final String interfaceName = networkIF.getName();
			final ITimeSource timesource = monitoringController.getTimeSource();
			final TimeUnit timeUnit = timesource.getTimeUnit();
			final NetworkStatistic currentNetworkStatistic = this.getCurrentNetworkStatistic(timesource, networkIF);

			if (!this.networkStatisticMap.containsKey(interfaceName)) {
				this.networkStatisticMap.putIfAbsent(interfaceName, currentNetworkStatistic);
			} else {
				final NetworkStatistic lastObservedNetworkStatistic = this.networkStatisticMap.get(interfaceName);
				final long timeDifference = currentNetworkStatistic.getTimestamp()
						- lastObservedNetworkStatistic.getTimestamp();
				if (timeDifference <= 0) {
					throw new IllegalStateException(
							"Timestamp of new observation should be strictly larger than the previous one.");
				}
				final long txBytesDifference = currentNetworkStatistic.getTxBytes()
						- lastObservedNetworkStatistic.getTxBytes();
				final long txCarrierDifference = currentNetworkStatistic.getTxCarrier()
						- lastObservedNetworkStatistic.getTxCarrier();
				final long txCollisionsDifference = currentNetworkStatistic.getTxCollisions()
						- lastObservedNetworkStatistic.getTxCollisions();
				final long txDroppedDifference = currentNetworkStatistic.getTxDropped()
						- lastObservedNetworkStatistic.getTxDropped();
				final long txErrorsDifference = currentNetworkStatistic.getTxErrors()
						- lastObservedNetworkStatistic.getTxErrors();
				final long txOverrunsDifference = currentNetworkStatistic.getTxOverruns()
						- lastObservedNetworkStatistic.getTxOverruns();
				final long txPacketsDifference = currentNetworkStatistic.getTxPackets()
						- lastObservedNetworkStatistic.getTxPackets();

				final long rxBytesDifference = currentNetworkStatistic.getRxBytes()
						- lastObservedNetworkStatistic.getRxBytes();
				final long rxDroppedDifference = currentNetworkStatistic.getRxDropped()
						- lastObservedNetworkStatistic.getRxDropped();
				final long rxErrorsDifference = currentNetworkStatistic.getRxErrors()
						- lastObservedNetworkStatistic.getRxErrors();
				final long rxFrameDifference = currentNetworkStatistic.getRxFrame()
						- lastObservedNetworkStatistic.getRxFrame();
				final long rxOverrunsDifference = currentNetworkStatistic.getRxOverruns()
						- lastObservedNetworkStatistic.getRxOverruns();
				final long rxPacketsDifference = currentNetworkStatistic.getRxPackets()
						- lastObservedNetworkStatistic.getRxPackets();

				final double txBytesPerSecond = txBytesDifference
						/ (double) TimeUnit.SECONDS.convert(timeDifference, timeUnit);
				final double txCarrierPerSecond = txCarrierDifference
						/ (double) TimeUnit.SECONDS.convert(timeDifference, timeUnit);
				final double txCollisionsPerSecond = txCollisionsDifference
						/ (double) TimeUnit.SECONDS.convert(timeDifference, timeUnit);
				final double txDroppedPerSecond = txDroppedDifference
						/ (double) TimeUnit.SECONDS.convert(timeDifference, timeUnit);
				final double txErrorsPerSecond = txErrorsDifference
						/ (double) TimeUnit.SECONDS.convert(timeDifference, timeUnit);
				final double txOverrunsPerSecond = txOverrunsDifference
						/ (double) TimeUnit.SECONDS.convert(timeDifference, timeUnit);
				final double txPacketsPerSecond = txPacketsDifference
						/ (double) TimeUnit.SECONDS.convert(timeDifference, timeUnit);

				final double rxBytesPerSecond = rxBytesDifference
						/ (double) TimeUnit.SECONDS.convert(timeDifference, timeUnit);
				final double rxDroppedPerSecond = rxDroppedDifference
						/ (double) TimeUnit.SECONDS.convert(timeDifference, timeUnit);
				final double rxErrorsPerSecond = rxErrorsDifference
						/ (double) TimeUnit.SECONDS.convert(timeDifference, timeUnit);
				final double rxFramePerSecond = rxFrameDifference
						/ (double) TimeUnit.SECONDS.convert(timeDifference, timeUnit);
				final double rxOverrunsPerSecond = rxOverrunsDifference
						/ (double) TimeUnit.SECONDS.convert(timeDifference, timeUnit);
				final double rxPacketsPerSecond = rxPacketsDifference
						/ (double) TimeUnit.SECONDS.convert(timeDifference, timeUnit);

				final NetworkUtilizationRecord r = new NetworkUtilizationRecord(currentNetworkStatistic.getTimestamp(),
						monitoringController.getHostname(), interfaceName, currentNetworkStatistic.getSpeed(),
						txBytesPerSecond, txCarrierPerSecond, txCollisionsPerSecond, txDroppedPerSecond,
						txErrorsPerSecond, txOverrunsPerSecond, txPacketsPerSecond, rxBytesPerSecond,
						rxDroppedPerSecond, rxErrorsPerSecond, rxFramePerSecond, rxOverrunsPerSecond,
						rxPacketsPerSecond);
				monitoringController.newMonitoringRecord(r);

				this.networkStatisticMap.put(interfaceName, currentNetworkStatistic);
			}
		}
	}

	private NetworkStatistic getCurrentNetworkStatistic(final ITimeSource timesource, final NetworkIF networkIF) {
		final long currentTimestamp = timesource.getTime();
		final long speed = networkIF.getSpeed();
		final long txBytes = networkIF.getBytesSent();
		// final long txCarrier = interfaceStat.getTxCarrier();
		final long txCarrier = 0;
		// final long txCollisions = interfaceStat.getTxCollisions();
		final long txCollisions = 0;
		// final long txDropped = interfaceStat.getTxDropped();
		final long txDropped = 0;
		final long txErrors = networkIF.getOutErrors();
		// final long txOverruns = interfaceStat.getTxOverruns();
		final long txOverruns = 0;
		final long txPackets = networkIF.getPacketsSent();
		final long rxBytes = networkIF.getBytesRecv();
		// final long rxDropped = interfaceStat.getRxDropped();
		final long rxDropped = 0;
		final long rxErrors = networkIF.getInErrors();
		// final long rxFrame = interfaceStat.getRxFrame();
		final long rxFrame = 0;
		// final long rxOverruns = interfaceStat.getRxOverruns();
		final long rxOverruns = 0;
		final long rxPackets = networkIF.getPacketsRecv();

		return new NetworkStatistic(currentTimestamp, speed, txBytes, txCarrier, txCollisions, txDropped, txErrors,
				txOverruns, txPackets, rxBytes, rxDropped, rxErrors, rxFrame, rxOverruns, rxPackets);
	}

	/**
	 * An inner class which stores network statistic for each observation.
	 *
	 * @author Teerat Pitakrat
	 *
	 * @since 1.12
	 */
	static class NetworkStatistic {
		private final long timestamp;
		private final long speed;
		private final long txBytes;
		private final long txCarrier;
		private final long txCollisions;
		private final long txDropped;
		private final long txErrors;
		private final long txOverruns;
		private final long txPackets;
		private final long rxBytes;
		private final long rxDropped;
		private final long rxErrors;
		private final long rxFrame;
		private final long rxOverruns;
		private final long rxPackets;

		public NetworkStatistic(final long timestamp, final long speed, final long txBytes, final long txCarrier,
				final long txCollisions, final long txDropped, final long txErrors, final long txOverruns,
				final long txPackets, final long rxBytes, final long rxDropped, final long rxErrors, final long rxFrame,
				final long rxOverruns, final long rxPackets) {
			this.timestamp = timestamp;
			this.speed = speed;
			this.txBytes = txBytes;
			this.txCarrier = txCarrier;
			this.txCollisions = txCollisions;
			this.txDropped = txDropped;
			this.txErrors = txErrors;
			this.txOverruns = txOverruns;
			this.txPackets = txPackets;
			this.rxBytes = rxBytes;
			this.rxDropped = rxDropped;
			this.rxErrors = rxErrors;
			this.rxFrame = rxFrame;
			this.rxOverruns = rxOverruns;
			this.rxPackets = rxPackets;
		}

		public long getTimestamp() {
			return this.timestamp;
		}

		public long getSpeed() {
			return this.speed;
		}

		public long getTxBytes() {
			return this.txBytes;
		}

		public long getTxCarrier() {
			return this.txCarrier;
		}

		public long getTxCollisions() {
			return this.txCollisions;
		}

		public long getTxDropped() {
			return this.txDropped;
		}

		public long getTxErrors() {
			return this.txErrors;
		}

		public long getTxOverruns() {
			return this.txOverruns;
		}

		public long getTxPackets() {
			return this.txPackets;
		}

		public long getRxBytes() {
			return this.rxBytes;
		}

		public long getRxDropped() {
			return this.rxDropped;
		}

		public long getRxErrors() {
			return this.rxErrors;
		}

		public long getRxFrame() {
			return this.rxFrame;
		}

		public long getRxOverruns() {
			return this.rxOverruns;
		}

		public long getRxPackets() {
			return this.rxPackets;
		}
	}
}
