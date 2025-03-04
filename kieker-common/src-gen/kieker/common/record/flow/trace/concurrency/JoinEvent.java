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
package kieker.common.record.flow.trace.concurrency;

import java.nio.BufferOverflowException;

import kieker.common.exception.RecordInstantiationException;
import kieker.common.record.flow.trace.AbstractTraceEvent;
import kieker.common.record.io.IValueDeserializer;
import kieker.common.record.io.IValueSerializer;


/**
 * @author Jan Waller
 * API compatibility: Kieker 2.0.0
 * 
 * @since 1.8
 */
public class JoinEvent extends AbstractTraceEvent  {			
	/** Descriptive definition of the serialization size of the record. */
	public static final int SIZE = TYPE_SIZE_LONG // IEventRecord.timestamp
			 + TYPE_SIZE_LONG // ITraceRecord.traceId
			 + TYPE_SIZE_INT // ITraceRecord.orderIndex
			 + TYPE_SIZE_LONG; // JoinEvent.joinedTraceId
	
	public static final Class<?>[] TYPES = {
		long.class, // IEventRecord.timestamp
		long.class, // ITraceRecord.traceId
		int.class, // ITraceRecord.orderIndex
		long.class, // JoinEvent.joinedTraceId
	};
	
	/** property name array. */
	public static final String[] VALUE_NAMES = {
		"timestamp",
		"traceId",
		"orderIndex",
		"joinedTraceId",
	};
	
	/** default constants. */
	public static final long JOINED_TRACE_ID = 0L;
	private static final long serialVersionUID = 5699964259392184696L;
	
	/** property declarations. */
	private final long joinedTraceId;
	
	/**
	 * Creates a new instance of this class using the given parameters.
	 * 
	 * @param timestamp
	 *            timestamp
	 * @param traceId
	 *            traceId
	 * @param orderIndex
	 *            orderIndex
	 * @param joinedTraceId
	 *            joinedTraceId
	 */
	public JoinEvent(final long timestamp, final long traceId, final int orderIndex, final long joinedTraceId) {
		super(timestamp, traceId, orderIndex);
		this.joinedTraceId = joinedTraceId;
	}


	/**
	 * @param deserializer
	 *            The deserializer to use
	 * @throws RecordInstantiationException 
	 *            when the record could not be deserialized
	 */
	public JoinEvent(final IValueDeserializer deserializer) throws RecordInstantiationException {
		super(deserializer);
		this.joinedTraceId = deserializer.getLong();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void serialize(final IValueSerializer serializer) throws BufferOverflowException {
		serializer.putLong(this.getTimestamp());
		serializer.putLong(this.getTraceId());
		serializer.putInt(this.getOrderIndex());
		serializer.putLong(this.getJoinedTraceId());
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Class<?>[] getValueTypes() {
		return TYPES; // NOPMD
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getValueNames() {
		return VALUE_NAMES; // NOPMD
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getSize() {
		return SIZE;
	}

	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(final Object obj) {
		if (obj == null) {
			return false;
		}
		if (obj == this) {
			return true;
		}
		if (obj.getClass() != this.getClass()) {
			return false;
		}
		
		final JoinEvent castedRecord = (JoinEvent) obj;
		if (this.getLoggingTimestamp() != castedRecord.getLoggingTimestamp()) {
			return false;
		}
		if (this.getTimestamp() != castedRecord.getTimestamp()) {
			return false;
		}
		if (this.getTraceId() != castedRecord.getTraceId()) {
			return false;
		}
		if (this.getOrderIndex() != castedRecord.getOrderIndex()) {
			return false;
		}
		if (this.getJoinedTraceId() != castedRecord.getJoinedTraceId()) {
			return false;
		}
		
		return true;
	}
	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode() {
		int code = 0;
		code += ((int)this.getTimestamp());
		code += ((int)this.getTraceId());
		code += ((int)this.getOrderIndex());
		code += ((int)this.getJoinedTraceId());
		
		return code;
	}
	
	public final long getJoinedTraceId() {
		return this.joinedTraceId;
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		String result = "JoinEvent: ";
		result += "timestamp = ";
		result += this.getTimestamp() + ", ";
		
		result += "traceId = ";
		result += this.getTraceId() + ", ";
		
		result += "orderIndex = ";
		result += this.getOrderIndex() + ", ";
		
		result += "joinedTraceId = ";
		result += this.getJoinedTraceId() + ", ";
		
		return result;
	}
}
