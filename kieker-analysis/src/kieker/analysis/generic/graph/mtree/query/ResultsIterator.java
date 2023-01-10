package kieker.analysis.generic.graph.mtree.query;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;

import kieker.analysis.generic.graph.mtree.MTree;
import kieker.analysis.generic.graph.mtree.nodes.AbstractNode;
import kieker.analysis.generic.graph.mtree.nodes.Entry;
import kieker.analysis.generic.graph.mtree.nodes.IndexItem;

public final class ResultsIterator<T> implements Iterator<ResultItem<T>> {

	private ResultItem<T> nextResultItem = null;
	private boolean finished = false;
	private final PriorityQueue<ItemWithDistances<AbstractNode<T>>> pendingQueue = new PriorityQueue<>();
	private double nextPendingMinDistance;
	private final PriorityQueue<ItemWithDistances<Entry<T>>> nearestQueue = new PriorityQueue<>();
	private int yieldedCount;
	private MTree<T> mtree;
	private Query<T> query;

	public ResultsIterator(final MTree<T> mtree, final Query<T> query) {
		this.mtree = mtree;
		this.query = query;
		if (this.mtree.getRoot() == null) {
			this.finished = true;
			return;
		}

		final double distance = this.mtree.getDistanceFunction().calculate(this.query.getData(), this.mtree.getRoot().getData());
		final double minDistance = Math.max(distance - this.mtree.getRoot().getRadius(), 0.0);

		this.pendingQueue.add(new ItemWithDistances<>(this.mtree.getRoot(), distance, minDistance));
		this.nextPendingMinDistance = minDistance;
	}

	@Override
	public boolean hasNext() {
		if (this.finished) {
			return false;
		}

		if (this.nextResultItem == null) {
			this.fetchNext();
		}

		if (this.nextResultItem == null) {
			this.finished = true;
			return false;
		} else {
			return true;
		}
	}

	@Override
	public ResultItem<T> next() {
		if (this.hasNext()) {
			final ResultItem<T> next = this.nextResultItem;
			this.nextResultItem = null;
			return next;
		} else {
			throw new NoSuchElementException();
		}
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	private void fetchNext() {
		assert !this.finished;

		if (this.finished || (this.yieldedCount >= this.query.getLimit())) {
			this.finished = true;
			return;
		}

		while (!this.pendingQueue.isEmpty() || !this.nearestQueue.isEmpty()) {
			if (this.prepareNextNearest()) {
				return;
			}

			assert !this.pendingQueue.isEmpty();

			final ItemWithDistances<AbstractNode<T>> pending = this.pendingQueue.poll();
			final AbstractNode<T> node = pending.item;

			for (final IndexItem<T> child : node.getChildren().values()) {
				if ((Math.abs(pending.distance - child.getDistanceToParent()) - child.getRadius()) <= this.query.getRange()) {
					final double childDistance = this.mtree.getDistanceFunction().calculate(this.query.getData(), child.getData());
					final double childMinDistance = Math.max(childDistance - child.getRadius(), 0.0);
					if (childMinDistance <= this.query.getRange()) {
						if (child instanceof Entry) {
							final Entry<T> entry = (Entry<T>) child;
							this.nearestQueue.add(new ItemWithDistances<>(entry, childDistance, childMinDistance));
						} else {
							final AbstractNode<T> childNode = (AbstractNode<T>) child;
							this.pendingQueue.add(new ItemWithDistances<>(childNode, childDistance, childMinDistance));
						}
					}
				}
			}

			if (this.pendingQueue.isEmpty()) {
				this.nextPendingMinDistance = Double.POSITIVE_INFINITY;
			} else {
				this.nextPendingMinDistance = this.pendingQueue.peek().minDistance;
			}
		}

		this.finished = true;
	}

	private boolean prepareNextNearest() {
		if (!this.nearestQueue.isEmpty()) {
			final ItemWithDistances<Entry<T>> nextNearest = this.nearestQueue.peek();
			if (nextNearest.distance <= this.nextPendingMinDistance) {
				this.nearestQueue.poll();
				this.nextResultItem = new ResultItem<T>(nextNearest.item.getData(), nextNearest.distance);
				++this.yieldedCount;
				return true;
			}
		}

		return false;
	}

	private class ItemWithDistances<U> implements Comparable<ItemWithDistances<U>> {
		private final U item;
		private final double distance;
		private final double minDistance;

		public ItemWithDistances(final U item, final double distance, final double minDistance) {
			this.item = item;
			this.distance = distance;
			this.minDistance = minDistance;
		}

		@Override
		public int compareTo(final ItemWithDistances<U> that) {
			if (this.minDistance < that.minDistance) {
				return -1;
			} else if (this.minDistance > that.minDistance) {
				return +1;
			} else {
				return 0;
			}
		}
	}
}
