package org.obda.query.domain;

import java.net.URI;

public interface PredicateFactory {

	/**
	 * Construct a {@link Predicate} object.
	 *
	 * @param name the name of the predicate (defined as a URI).
	 * @param arity the number of elements inside the predicate.
	 * @return a predicate object.
	 */
	public Predicate createPredicate(URI name, int arity);
}
