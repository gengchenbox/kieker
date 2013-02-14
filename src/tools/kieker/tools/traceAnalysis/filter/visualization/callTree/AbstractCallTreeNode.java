/***************************************************************************
 * Copyright 2012 Kieker Project (http://kieker-monitoring.net)
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

package kieker.tools.traceAnalysis.filter.visualization.callTree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import kieker.tools.traceAnalysis.filter.visualization.graph.AbstractVertex;
import kieker.tools.traceAnalysis.filter.visualization.graph.IOriginRetentionPolicy;
import kieker.tools.traceAnalysis.systemModel.MessageTrace;

/**
 * This is an abstract base for a single node within a call tree.
 * 
 * @param <T>
 *            The type of the entity to be stored in the node.
 * 
 * @author Andre van Hoorn
 */
public abstract class AbstractCallTreeNode<T> extends AbstractVertex<AbstractCallTreeNode<T>, WeightedDirectedCallTreeEdge<T>, MessageTrace> {

	private final T entity;
	private final int id;

	private final boolean rootNode;

	private final List<WeightedDirectedCallTreeEdge<T>> childEdges = Collections.synchronizedList(new ArrayList<WeightedDirectedCallTreeEdge<T>>());

	public AbstractCallTreeNode(final int id, final T entity, final boolean rootNode, final MessageTrace origin, final IOriginRetentionPolicy originPolicy) {
		super(origin, originPolicy);
		this.id = id;
		this.rootNode = rootNode;
		this.entity = entity;
	}

	/**
	 * Delivers the stored entity.
	 * 
	 * @return The content of this node.
	 */
	public final T getEntity() {
		return this.entity;
	}

	/**
	 * Delivers the child edges.
	 * 
	 * @return A collection containing the child edges.
	 */
	public final Collection<WeightedDirectedCallTreeEdge<T>> getChildEdges() {
		return this.childEdges;
	}

	/** Append edge to *sorted* list of children. */
	protected final void appendChildEdge(final WeightedDirectedCallTreeEdge<T> destination) {
		this.childEdges.add(destination);
	}

	// TODO: Dirty hack, Object should be T.
	public abstract AbstractCallTreeNode<T> newCall(Object destination, MessageTrace origin, IOriginRetentionPolicy originPolicy);

	public final int getId() {
		return this.id;
	}

	/**
	 * Tells whether the current node is a root node.
	 * 
	 * @return true if and only if the current node has to be interpreted as a root node.
	 */
	public final boolean isRootNode() {
		return this.rootNode;
	}

	@Override
	public Collection<WeightedDirectedCallTreeEdge<T>> getOutgoingEdges() {
		return this.getChildEdges();
	}
}
