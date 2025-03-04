/***************************************************************************
 * Copyright 2022 Kieker Project (https://kieker-monitoring.net)
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

package kieker.common.exception;

/**
 * This exception should be thrown if an illegal configuration (parameter) is detected by a configurable component.
 *
 * @author Holger Knoche
 * @since 1.13
 *
 */
public class InvalidConfigurationException extends RuntimeException {

	private static final long serialVersionUID = -7683665726823960846L;

	/**
	 * Creates a new exception with the given message.
	 * 
	 * @param message
	 *            The message to associate with this exception
	 */
	public InvalidConfigurationException(final String message) {
		super(message);
	}

	/**
	 * Creates a new exception with the given message and cause.
	 * 
	 * @param message
	 *            The message to associate with this exception
	 * @param cause
	 *            The cause for this exception
	 */
	public InvalidConfigurationException(final String message, final Throwable cause) {
		super(message, cause);
	}

}
