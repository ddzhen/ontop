import java.net.URI;
import java.util.List;
import java.util.Vector;

import org.junit.Test;
import org.obda.query.domain.Atom;
import org.obda.query.domain.CQIE;
import org.obda.query.domain.DatalogProgram;
import org.obda.query.domain.Predicate;
import org.obda.query.domain.PredicateFactory;
import org.obda.query.domain.Term;
import org.obda.query.domain.TermFactory;
import org.obda.query.domain.imp.AtomImpl;
import org.obda.query.domain.imp.BasicPredicateFactoryImpl;
import org.obda.query.domain.imp.CQIEImpl;
import org.obda.query.domain.imp.DatalogProgramImpl;
import org.obda.query.domain.imp.TermFactoryImpl;
import org.obda.query.domain.imp.UndistinguishedVariable;
import org.obda.reformulation.dllite.DLRPerfectReformulator;
import org.obda.reformulation.dllite.QueryRewriter;
import org.obda.reformulation.domain.Assertion;

import junit.framework.TestCase;

public class UnificationTest2 extends TestCase {

	/**
	 * Test method for
	 * {@link org.obda.reformulation.dllite.DLRPerfectReformulator#rewrite(org.obda.query.domain.Query)}
	 * .
	 * 
	 * Check if MGU generation/application works properly with multiple atoms sharing variables
	 * 
	 * q(x,y) :- R(x,#) R(#,y), S(x,#)
	 * 
	 * @throws Exception
	 */
	@Test
	public void test_1() throws Exception {

		TermFactory factory = TermFactoryImpl.getInstance();
		PredicateFactory predFac = BasicPredicateFactoryImpl.getInstance();

		Term t1 = factory.createVariable("x");
		Term t2 = factory.createVariable("y");
		Term t3 = factory.createVariable("x");

		Predicate r1 = predFac.getPredicate(URI.create("R"), 2);
		Predicate r2 = predFac.getPredicate(URI.create("R"), 2);
		Predicate s = predFac.getPredicate(URI.create("S"), 2);
		Predicate p = predFac.getPredicate(URI.create("p"), 2);

		List<Term> terms1 = new Vector<Term>();
		terms1.add(t1);
		terms1.add(new UndistinguishedVariable());
		List<Term> terms2 = new Vector<Term>();
		terms2.add(new UndistinguishedVariable());
		terms2.add(t2);
		List<Term> terms3 = new Vector<Term>();
		terms3.add(t3);
		terms3.add(new UndistinguishedVariable());
		List<Term> terms4 = new Vector<Term>();
		terms4.add(t3.copy());
		terms4.add(t2.copy());

		Atom a1 = new AtomImpl(r1, terms1);
		Atom a2 = new AtomImpl(r2, terms2);
		Atom a3 = new AtomImpl(s, terms3);
		Atom head = new AtomImpl(p, terms4);

		Vector<Atom> body = new Vector<Atom>();
		body.add(a1);
		body.add(a2);
		body.add(a3);
		CQIE query = new CQIEImpl(head, body, true);
		DatalogProgram prog = new DatalogProgramImpl();
		prog.appendRule(query);

		List<Assertion> list = new Vector<Assertion>();
		QueryRewriter rew = new DLRPerfectReformulator(list);
		DatalogProgram aux = (DatalogProgram) rew.rewrite(prog);

		assertEquals(2, aux.getRules().size());
		// note: aux.getRules().get(0) should be the original one
		CQIE cq = aux.getRules().get(1);
		List<Atom> newbody = cq.getBody();

		assertEquals(2, newbody.size());
		Atom at1 = newbody.get(0);
		Atom at2 = newbody.get(1);

		Term term1 = at1.getTerms().get(0);
		Term term2 = at1.getTerms().get(1);
		Term term3 = at2.getTerms().get(0);
		Term term4 = at2.getTerms().get(1);

		assertEquals("x", term1.getName());
		assertEquals("y", term2.getName());
		assertEquals("x", term3.getName());
		assertEquals("#", term4.getName());

	}

}
