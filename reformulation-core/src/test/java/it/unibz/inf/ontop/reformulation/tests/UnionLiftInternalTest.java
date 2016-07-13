package it.unibz.inf.ontop.reformulation.tests;


import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import it.unibz.inf.ontop.model.*;
import it.unibz.inf.ontop.model.impl.AtomPredicateImpl;
import it.unibz.inf.ontop.model.impl.OBDADataFactoryImpl;
import it.unibz.inf.ontop.model.impl.URITemplatePredicateImpl;
import it.unibz.inf.ontop.owlrefplatform.core.basicoperations.ImmutableSubstitutionImpl;
import it.unibz.inf.ontop.pivotalrepr.*;
import it.unibz.inf.ontop.pivotalrepr.equivalence.IQSyntacticEquivalenceChecker;
import it.unibz.inf.ontop.pivotalrepr.impl.*;
import it.unibz.inf.ontop.pivotalrepr.impl.tree.DefaultIntermediateQueryBuilder;
import it.unibz.inf.ontop.pivotalrepr.proposal.impl.UnionLiftProposalImpl;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertTrue;

public class UnionLiftInternalTest {

    private final static OBDADataFactory DATA_FACTORY = OBDADataFactoryImpl.getInstance();

    private static Constant URI_TEMPLATE_STR_1 =  DATA_FACTORY.getConstantLiteral("http://example.org/ds1/{}");
    private static URITemplatePredicate URI_PREDICATE =  new URITemplatePredicateImpl(2);

    private static AtomPredicate P1_PREDICATE = new AtomPredicateImpl("p1", 1);
    private static AtomPredicate TABLE_1 = new AtomPredicateImpl("table1", 1);
    private static AtomPredicate TABLE_2 = new AtomPredicateImpl("table2", 1);
    private static AtomPredicate TABLE_3 = new AtomPredicateImpl("table3", 2);
    private static AtomPredicate TABLE_4 = new AtomPredicateImpl("table4", 2);

    private static Variable X = DATA_FACTORY.getVariable("x");
    private static Variable Y = DATA_FACTORY.getVariable("y");
    private static Variable Z = DATA_FACTORY.getVariable("z");
    private static Variable T = DATA_FACTORY.getVariable("t");
    private static Variable A = DATA_FACTORY.getVariable("a");
    private static Variable B = DATA_FACTORY.getVariable("b");
    private static Variable C = DATA_FACTORY.getVariable("c");
    private static Variable D = DATA_FACTORY.getVariable("d");
    private static Variable E = DATA_FACTORY.getVariable("e");
    private static Variable F = DATA_FACTORY.getVariable("f");

    private static DistinctVariableOnlyDataAtom ROOT_CONSTRUCTION_NODE_ATOM =
            DATA_FACTORY.getDistinctVariableOnlyDataAtom(
            P1_PREDICATE, ImmutableList.of(X, Y, Z));

    private static DistinctVariableOnlyDataAtom TABLE1_ATOM = DATA_FACTORY.getDistinctVariableOnlyDataAtom(
            P1_PREDICATE, ImmutableList.of(X));
    private static DistinctVariableOnlyDataAtom TABLE2_ATOM = DATA_FACTORY.getDistinctVariableOnlyDataAtom(
            P1_PREDICATE, ImmutableList.of(X));
    private static DistinctVariableOnlyDataAtom TABLE3_ATOM = DATA_FACTORY.getDistinctVariableOnlyDataAtom(
            P1_PREDICATE, ImmutableList.of(X, Y));
    private static DistinctVariableOnlyDataAtom TABLE4_ATOM = DATA_FACTORY.getDistinctVariableOnlyDataAtom(
            P1_PREDICATE, ImmutableList.of(Y, Z));

    private final MetadataForQueryOptimization metadata;

    public UnionLiftInternalTest() {
        this.metadata = initMetadata();
    }

    private static MetadataForQueryOptimization initMetadata() {
        ImmutableMultimap.Builder<AtomPredicate, ImmutableList<Integer>> uniqueKeyBuilder = ImmutableMultimap.builder();
        return new MetadataForQueryOptimizationImpl(uniqueKeyBuilder.build(), new UriTemplateMatcher());
    }


    @Test
    public void unionLiftInternalTest1 () throws EmptyQueryException {

        /**
         * Original Query
         */
        IntermediateQueryBuilder originalBuilder = new DefaultIntermediateQueryBuilder(metadata);

        ConstructionNode rootConstructionNode = new ConstructionNodeImpl(ROOT_CONSTRUCTION_NODE_ATOM.getVariables(),
                new ImmutableSubstitutionImpl<>(ImmutableMap.of()), Optional.empty());

        InnerJoinNode joinNode = new InnerJoinNodeImpl(Optional.empty());
        LeftJoinNode leftJoinNode = new LeftJoinNodeImpl(Optional.empty());

        ConstructionNode table4Construction = new ConstructionNodeImpl(TABLE4_ATOM.getVariables(),
                new ImmutableSubstitutionImpl<>(ImmutableMap.of(Y, generateURI1(E), Z, generateURI1(F))),
                Optional.empty());
        ExtensionalDataNode table4DataNode = new ExtensionalDataNodeImpl(DATA_FACTORY.getDataAtom(TABLE_4, E, F));

        UnionNode unionNode = new UnionNodeImpl(ImmutableSet.of(X));

        ConstructionNode table1Construction = new ConstructionNodeImpl(TABLE1_ATOM.getVariables(),
                new ImmutableSubstitutionImpl<>(ImmutableMap.of(X, generateURI1(A))), Optional.empty());
        ExtensionalDataNode table1DataNode = new ExtensionalDataNodeImpl(DATA_FACTORY.getDataAtom(TABLE_1, A));

        ConstructionNode table2Construction = new ConstructionNodeImpl(TABLE2_ATOM.getVariables(),
                new ImmutableSubstitutionImpl<>(ImmutableMap.of(X, generateURI1(B))), Optional.empty());
        ExtensionalDataNode table2DataNode = new ExtensionalDataNodeImpl(DATA_FACTORY.getDataAtom(TABLE_2, B));

        ConstructionNode table3Construction = new ConstructionNodeImpl(TABLE3_ATOM.getVariables(),
                new ImmutableSubstitutionImpl<>(ImmutableMap.of(X, generateURI1(C), Y, generateURI1(D))),
                Optional.empty());
        ExtensionalDataNode table3DataNode = new ExtensionalDataNodeImpl(DATA_FACTORY.getDataAtom(TABLE_3, C, D));

        originalBuilder.init(ROOT_CONSTRUCTION_NODE_ATOM, rootConstructionNode);
        originalBuilder.addChild(rootConstructionNode, joinNode);
        originalBuilder.addChild(joinNode, leftJoinNode);
        originalBuilder.addChild(joinNode, table4Construction);
        originalBuilder.addChild(table4Construction, table4DataNode);

        originalBuilder.addChild(leftJoinNode, unionNode, NonCommutativeOperatorNode.ArgumentPosition.LEFT);
        originalBuilder.addChild(leftJoinNode, table3Construction, NonCommutativeOperatorNode.ArgumentPosition.RIGHT);
        originalBuilder.addChild(unionNode, table1Construction);
        originalBuilder.addChild(unionNode, table2Construction);

        originalBuilder.addChild(table3Construction, table3DataNode);
        originalBuilder.addChild(table2Construction, table2DataNode);
        originalBuilder.addChild(table1Construction, table1DataNode);

        IntermediateQuery originalQuery = originalBuilder.build();

        System.out.println("\n Original query: \n" +  originalQuery);

        IntermediateQuery optimizedQuery = originalQuery.applyProposal(new UnionLiftProposalImpl(unionNode, leftJoinNode))
                .getResultingQuery();

        /**
         * Expected Query
         */
        IntermediateQueryBuilder expectedBuilder = new DefaultIntermediateQueryBuilder(metadata);

        InnerJoinNode joinNodeExpected = new InnerJoinNodeImpl(Optional.empty());
        UnionNode unionNodeExpected = new UnionNodeImpl(ImmutableSet.of(X, Y));
        LeftJoinNode leftJoinNode1 = new LeftJoinNodeImpl(Optional.empty());
        LeftJoinNode leftJoinNode2 = new LeftJoinNodeImpl(Optional.empty());

        ConstructionNode table3ConstructionExpected = new ConstructionNodeImpl(TABLE3_ATOM.getVariables(),
                new ImmutableSubstitutionImpl<>(ImmutableMap.of(X, generateURI1(C), Y, generateURI1(D))),
                Optional.empty());
        ExtensionalDataNode table3DataNodeExpected = new ExtensionalDataNodeImpl(DATA_FACTORY.getDataAtom(TABLE_3, C, D));

        expectedBuilder.init(ROOT_CONSTRUCTION_NODE_ATOM, rootConstructionNode);
        expectedBuilder.addChild(rootConstructionNode, joinNodeExpected);
        expectedBuilder.addChild(joinNodeExpected, unionNodeExpected);
        expectedBuilder.addChild(joinNodeExpected, table4Construction);
        expectedBuilder.addChild(unionNodeExpected, leftJoinNode1);
        expectedBuilder.addChild(unionNodeExpected, leftJoinNode2);
        expectedBuilder.addChild(leftJoinNode1, table1Construction, NonCommutativeOperatorNode.ArgumentPosition.LEFT);
        expectedBuilder.addChild(leftJoinNode1, table3Construction, NonCommutativeOperatorNode.ArgumentPosition.RIGHT);
        expectedBuilder.addChild(leftJoinNode2, table2Construction, NonCommutativeOperatorNode.ArgumentPosition.LEFT);
        expectedBuilder.addChild(leftJoinNode2, table3ConstructionExpected, NonCommutativeOperatorNode.ArgumentPosition.RIGHT);
        expectedBuilder.addChild(table1Construction, table1DataNode);
        expectedBuilder.addChild(table2Construction, table2DataNode);
        expectedBuilder.addChild(table3Construction, table3DataNode);
        expectedBuilder.addChild(table4Construction, table4DataNode);
        expectedBuilder.addChild(table3ConstructionExpected, table3DataNodeExpected);

        IntermediateQuery expectedQuery = expectedBuilder.build();

        System.out.println("\n Optimized query: \n" +  optimizedQuery);
        System.out.println("\n Expected query: \n" +  expectedQuery);

        assertTrue(IQSyntacticEquivalenceChecker.areEquivalent(optimizedQuery, expectedQuery));

    }

    private static ImmutableFunctionalTerm generateURI1(VariableOrGroundTerm argument) {
        return DATA_FACTORY.getImmutableFunctionalTerm(URI_PREDICATE, URI_TEMPLATE_STR_1, argument);
    }


}
