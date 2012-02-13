/***************************************************************************
 * Copyright 2011 by
 *  + Christian-Albrechts-University of Kiel
 *    + Department of Computer Science
 *      + Software Engineering Group 
 *  and others.
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

package kieker.common.record.flow;

/**
 * @author Jan Waller
 */
public abstract class TraceEvent extends Event {
	private static final long serialVersionUID = 1L;

	// TODO: sessionId?

	private final long traceId;
	private final int orderIndex;

	public TraceEvent(final long timestamp, final long traceId, final int orderIndex) {
		super(timestamp);
		this.traceId = traceId;
		this.orderIndex = orderIndex;
	}

	public TraceEvent(final Object[] values, final Class<?>[] valueTypes) {
		super(values, valueTypes); // values[0]
		this.traceId = (Long) values[1];
		this.orderIndex = (Integer) values[2];
	}

	public final long getTraceId() {
		return this.traceId;
	}

	public final int getOrderIndex() {
		return this.orderIndex;
	}
}