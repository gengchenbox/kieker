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

package kieker.tools.traceAnalysis.systemModel.repository;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Map;

import kieker.tools.traceAnalysis.systemModel.ComponentType;

/**
 * 
 * @author Andre van Hoorn
 */
public class TypeRepository extends AbstractSystemSubRepository {

	public static final ComponentType ROOT_COMPONENT = new ComponentType(AbstractSystemSubRepository.ROOT_ELEMENT_ID, "$");

	private final Map<String, ComponentType> componentTypesByName = new Hashtable<String, ComponentType>(); // NOPMD (UseConcurrentHashMap)
	private final Map<Integer, ComponentType> componentTypesById = new Hashtable<Integer, ComponentType>(); // NOPMD (UseConcurrentHashMap)

	public TypeRepository(final SystemModelRepository systemFactory) {
		super(systemFactory);
	}

	/**
	 * Returns the instance for the passed namedIdentifier; null if no instance
	 * with this namedIdentifier.
	 */
	public final ComponentType lookupComponentTypeByNamedIdentifier(final String namedIdentifier) {
		synchronized (this) {
			return this.componentTypesByName.get(namedIdentifier);
		}
	}

	/**
	 * Creates and registers a component type that has not been registered yet.
	 * 
	 * @param namedIdentifier
	 * @param fullqualifiedName
	 * @return the created component type
	 * @throws IllegalArgumentException
	 *             if a component type with the given
	 *             namedIdentifier has already been registered
	 */
	public final ComponentType createAndRegisterComponentType(final String namedIdentifier, final String fullqualifiedName) {
		final ComponentType newInst;
		synchronized (this) {
			if (this.componentTypesByName.containsKey(namedIdentifier)) {
				throw new IllegalArgumentException("Element with name " + namedIdentifier + "exists already");
			}
			final int id = this.getAndIncrementNextId();
			newInst = new ComponentType(id, fullqualifiedName);
			this.componentTypesById.put(id, newInst);
			this.componentTypesByName.put(namedIdentifier, newInst);
		}
		return newInst;
	}

	/**
	 * Returns a collection of all registered component types.
	 * 
	 * @return a collection of all registered component types.
	 */
	public final Collection<ComponentType> getComponentTypes() {
		synchronized (this) {
			return this.componentTypesById.values();
		}
	}
}
