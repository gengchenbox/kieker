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

package kieker.analysis.exception;

/**
 * 
 * @author Andre van Hoorn
 * 
 * @since 1.2
 */
public class EventProcessingException extends Exception {

	private static final long serialVersionUID = 345L;

	/**
	 * Creates a new instance of this class using the given parameters.
	 * 
	 * @param message
	 *            The message of the exception.
	 * @param cause
	 *            The cause of the exception.
	 */
	public EventProcessingException(final String message, final Throwable cause) {
		super(message, cause);
	}

	/**
	 * Creates a new instance of this class using the given parameters.
	 * 
	 * @param message
	 *            The message of the exception.
	 */
	public EventProcessingException(final String message) {
		super(message);
	}
}
