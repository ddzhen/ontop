package org.semanticweb.ontop.owlrefplatform.core.unfolding;

import fj.data.List;
import org.semanticweb.ontop.model.CQIE;
import org.semanticweb.ontop.model.Function;
import org.semanticweb.ontop.model.Predicate;
import org.semanticweb.ontop.model.TypeProposal;

/**
 * For URI templates using more than one variable.
 *
 * TODO: implement it
 */
public class MultiVariateUriTemplateTypeProposal implements TypeProposal {
    @Override
    public Function getProposedHead() {
        return null;
    }

    @Override
    public Predicate getPredicate() {
        return null;
    }

    @Override
    public List<CQIE> applyType(List<CQIE> initialRules) {
        return null;
    }

    @Override
    public List<CQIE> removeType(List<CQIE> initialRules) {
        return null;
    }
}
