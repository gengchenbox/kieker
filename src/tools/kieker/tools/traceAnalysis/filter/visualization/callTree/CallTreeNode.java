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

import kieker.tools.traceAnalysis.systemModel.AllocationComponent;
import kieker.tools.traceAnalysis.systemModel.Operation;

/**
 * This class represents a single node within the call tree.
 * 
 * @author Andre van Hoorn
 */
public class CallTreeNode {

	private final CallTreeNode parent;
	private final List<CallTreeNode> children = Collections.synchronizedList(new ArrayList<CallTreeNode>());
	private final CallTreeOperationHashKey opInfo;

	/**
	 * Creates a new instance of this class using the given parameters.
	 * 
	 * @param parent
	 *            The parent of this node. If this is null, the node will be interpreted as a root node.
	 * @param opInfo
	 *            The info to be stored in this node.
	 */
	public CallTreeNode(final CallTreeNode parent, final CallTreeOperationHashKey opInfo) {
		this.parent = parent;
		if (opInfo == null) {
			throw new IllegalArgumentException("opInfo must not be null");
		}
		this.opInfo = opInfo;
	}

	/**
	 * Delivers a collection containing the child nodes.
	 * 
	 * @return The children of this node.
	 */
	public final Collection<CallTreeNode> getChildren() {
		return this.children;
	}

	/** Creates a new child and adds it to the nodes list of children. */
	public final CallTreeNode createNewChild(final AllocationComponent allocationComponent, final Operation operation) {
		final CallTreeOperationHashKey k = new CallTreeOperationHashKey(allocationComponent, operation);
		final CallTreeNode node = new CallTreeNode(this, k);
		this.children.add(node);
		return node;
	}

	/**
	 * Returns the child node with given operation, name, and vmName. The node is created if it doesn't exist.
	 */
	public final CallTreeNode getChild(final AllocationComponent allocationComponent, final Operation operation) {
		final CallTreeOperationHashKey k = new CallTreeOperationHashKey(allocationComponent, operation);
		CallTreeNode node = null;
		for (final CallTreeNode n : this.children) {
			if (n.opInfo.equals(k)) {
				node = n;
			}
		}
		if (node == null) {
			node = new CallTreeNode(this, k);
			this.children.add(node);
		}
		return node;
	}

	public final AllocationComponent getAllocationComponent() {
		return this.opInfo.getAllocationComponent();
	}

	public final Operation getOperation() {
		return this.opInfo.getOperation();
	}

	/**
	 * Delivers the parent of this node.
	 * 
	 * @return The parent.
	 */
	public final CallTreeNode getParent() {
		return this.parent;
	}

	/**
	 * Tells whether the current node is the root or not.
	 * 
	 * @return true if and only if this node should be interpreted as a root.
	 */
	public final boolean isRootNode() {
		return this.parent == null;
	}
}
