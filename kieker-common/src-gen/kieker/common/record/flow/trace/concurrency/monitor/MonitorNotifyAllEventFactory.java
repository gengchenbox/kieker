/***************************************************************************
 * Copyright 2023 Kieker Project (http://kieker-monitoring.net)
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
package kieker.common.record.flow.trace.concurrency.monitor;


import kieker.common.exception.RecordInstantiationException;
import kieker.common.record.factory.IRecordFactory;
import kieker.common.record.io.IValueDeserializer;

/**
 * @author Jan Waller
 * 
 * @since 1.8
 */
public final class MonitorNotifyAllEventFactory implements IRecordFactory<MonitorNotifyAllEvent> {
	

	@Override
	public MonitorNotifyAllEvent create(final IValueDeserializer deserializer) throws RecordInstantiationException {
		return new MonitorNotifyAllEvent(deserializer);
	}


	@Override
	public String[] getValueNames() {
		return MonitorNotifyAllEvent.VALUE_NAMES; // NOPMD
	}

	@Override
	public Class<?>[] getValueTypes() {
		return MonitorNotifyAllEvent.TYPES; // NOPMD
	}

	public int getRecordSizeInBytes() {
		return MonitorNotifyAllEvent.SIZE;
	}
}
