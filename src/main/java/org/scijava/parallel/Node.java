package org.scijava.parallel;

import java.util.LinkedList;
import java.util.List;

public abstract class Node<T> {

	private final List<Node<T>> successors = new LinkedList<>();

	private final T content;

	public Node(final T content) {
		this.content = content;
	}

	public T getContent() {
		return content;
	}

	public List<Node<T>> getSuccessors() {
		return successors;
	}

	public void addSuccessor(final Node<T> successiveNode) {
		successors.add(successiveNode);
	}

	public void addSuccessors(final List<Node<T>> successiveNodes) {
		successors.addAll(successiveNodes);
	}

	public void mergeNodes(final List<Node<T>> nodes) {
		nodes.forEach(t -> t.addSuccessor(this));
	}

	// TODO: Do we want to go for the builder pattern and allow chaining when adding
	// single successors?

	// TODO: Create a method for inserting a node between two other nodes

	// TODO: Create a method which would check whether we are still acyclic
}
