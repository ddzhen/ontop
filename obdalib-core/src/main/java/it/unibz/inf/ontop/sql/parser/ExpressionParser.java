package it.unibz.inf.ontop.sql.parser;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import it.unibz.inf.ontop.model.*;
import it.unibz.inf.ontop.model.Function;
import it.unibz.inf.ontop.model.impl.OBDADataFactoryImpl;
import it.unibz.inf.ontop.sql.QualifiedAttributeID;
import it.unibz.inf.ontop.sql.QuotedID;
import it.unibz.inf.ontop.sql.QuotedIDFactory;
import it.unibz.inf.ontop.sql.RelationID;
import it.unibz.inf.ontop.sql.parser.exceptions.InvalidSelectQueryEcxeption;
import it.unibz.inf.ontop.sql.parser.exceptions.UnsupportedSelectQueryException;
import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.arithmetic.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.SubSelect;

import java.util.List;
import java.util.function.BinaryOperator;
import java.util.function.UnaryOperator;

/**
 * Created by Roman Kontchakov on 10/11/2016.
 *
 */

public class ExpressionParser implements java.util.function.Function<ImmutableMap<QualifiedAttributeID, Variable>, ImmutableList<Function>> {

    private final QuotedIDFactory idfac;
    private final Expression expression;

    private static final OBDADataFactory FACTORY = OBDADataFactoryImpl.getInstance();

    public   ExpressionParser(QuotedIDFactory idfac, Expression expression) {
        this.idfac = idfac;
        this.expression = expression;
    }

    public static ExpressionParser empty() {
        return new ExpressionParser (null, null) {
            @Override
            public ImmutableList<Function> apply(ImmutableMap<QualifiedAttributeID, Variable> attributes) {
                return ImmutableList.of();
            }
        };
    }

    @Override
    public ImmutableList<Function> apply(ImmutableMap<QualifiedAttributeID, Variable> attributes) {

        ExpressionVisitorImpl visitor = new ExpressionVisitorImpl(attributes);

        Expression current = expression;

        if (current instanceof AndExpression) {
            ImmutableList.Builder<Function> builder = ImmutableList.builder();
            do {
                AndExpression and = (AndExpression) current;
                // for a sequence of AND operations, JSQLParser makes the right argument simple
                builder.add(translateIntoFunction(visitor, and.getRightExpression()));
                // and the left argument complex (nested AND)
                current = and.getLeftExpression();
            } while (current instanceof AndExpression);

            builder.add(translateIntoFunction(visitor, current));
            return builder.build().reverse();
        }
        return ImmutableList.of(translateIntoFunction(visitor, current));
    }

    private static Function translateIntoFunction(ExpressionVisitorImpl visitor, Expression current) {
        Term t = visitor.translate(current);
        if (t instanceof Function)
            return (Function)t;

        // TODO: better handling of the situation?
        throw new RuntimeException("");
    }


    // TODO: this class is being reviewed


    /**
     * This visitor class converts the SQL Expression to a Function
     *
     * Exceptions
     *      - UnsupportedOperationException: an internal error (due to the unexpected bahaviour of JSQLparser)
     *      - InvalidSelectQueryEcxeption: the input is not a valid mapping query
     *      - UnsupportedSelectQueryException: the input cannot be converted into a CQ and needs to be wrapped
     *
     */
    private class ExpressionVisitorImpl implements ExpressionVisitor {

        private final ImmutableMap<QualifiedAttributeID, Variable> attributes;

        private Term result; // CAREFUL: this variable gets reset in each visit method implementation

        ExpressionVisitorImpl(ImmutableMap<QualifiedAttributeID, Variable> attributes) {
            this.attributes = attributes;
        }

        private Term translate(Expression expression) {
            expression.accept(this);
            return this.result;
        }

        private void visitBinaryExpression(BinaryExpression expression, BinaryOperator<Term> op) {
            Term leftTerm = translate(expression.getLeftExpression());
            Term rightTerm = translate(expression.getRightExpression());
            Term expTerm = op.apply(leftTerm, rightTerm);

            result = expression.isNot() ? FACTORY.getFunctionNOT(expTerm) : expTerm;
        }

        // CAREFUL: the first argument is NOT the composite term, but rather its argument
        private void visitUnaryExpression(Expression arg, boolean isNot, UnaryOperator<Term> op) {
            Term term = translate(arg);
            Term expTerm = op.apply(term);

            result = isNot ? FACTORY.getFunctionNOT(expTerm) : expTerm;
        }


        @Override
        public void visit(NullValue nullValue) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(net.sf.jsqlparser.expression.Function func) {
            String functionName = func.getName().toLowerCase();
            List<Expression> expressions = func.getParameters().getExpressions();

            if (functionName.equals("regexp_like")) {
                if (expressions.size() == 2 || expressions.size() == 3) {
                    Term t1 = translate(expressions.get(0));  // a source string
                    Term t2 = translate(expressions.get(1)); // a regex pattern

                    // the third parameter is optional for match_parameter in regexp_like
                    Term t3 = (expressions.size() == 3)
                            ? translate(expressions.get(2))
                            : FACTORY.getConstantLiteral("");

                    result = FACTORY.getFunction(ExpressionOperation.REGEX, t1, t2, t3);
                }
                else
                    throw new InvalidSelectQueryEcxeption("Wrong number of arguments for SQL function REGEX_LIKE", func);
            }
            else if (functionName.endsWith("replace")) {
                if (expressions.size() == 2 || expressions.size() == 3) {
                    Term t1 = translate(expressions.get(0));
                    Term t2 = translate(expressions.get(1)); // second parameter is a string

                    // Term t3 is optional: no string means delete occurrences of second param
                    Term t3 =  (expressions.size() == 3)
                            ? translate(expressions.get(2))
                            : FACTORY.getConstantLiteral("");

                    result = FACTORY.getFunction(ExpressionOperation.REPLACE, t1, t2, t3,
                            FACTORY.getConstantLiteral("")); // the 4th argument is flags
                }
                else
                    throw new InvalidSelectQueryEcxeption("Wrong number of arguments in SQL function REPLACE", func);
            }
            else if (functionName.endsWith("concat")){

                int nParameters = expressions.size();
                Function topConcat = null;
                // TODO: this loop is incorrect for size > 3, fix it
                for (int i = 0; i < nParameters; i += 2) {
                    if (topConcat == null) {
                        Term t1 = translate(expressions.get(i));
                        Term t2 = translate(expressions.get(i + 1));
                        topConcat = FACTORY.getFunction(ExpressionOperation.CONCAT, t1, t2);
                    }
                    else {
                        Term t2 = translate(expressions.get(i));
                        topConcat = FACTORY.getFunction(ExpressionOperation.CONCAT, topConcat, t2);
                    }
                }
                result = topConcat;
            }
            else
                throw new UnsupportedSelectQueryException("Unsupported function ", func);
        }

        /*
                CONSTANT EXPRESSIONS
         */

        @Override
        public void visit(DoubleValue expression) {
            result = FACTORY.getConstantLiteral(expression.toString(), Predicate.COL_TYPE.DOUBLE);
        }

        @Override
        public void visit(LongValue expression) {
            result = FACTORY.getConstantLiteral(expression.getStringValue(), Predicate.COL_TYPE.LONG);
        }

        @Override
        public void visit(StringValue expression) {
            result = FACTORY.getConstantLiteral(expression.getValue(), Predicate.COL_TYPE.STRING);
        }

        @Override
        public void visit(DateValue expression) {
            result = FACTORY.getConstantLiteral(expression.getValue().toString(), Predicate.COL_TYPE.DATE);
        }

        @Override
        public void visit(TimeValue expression) {
            result = FACTORY.getConstantLiteral(expression.getValue().toString(), Predicate.COL_TYPE.TIME);
        }

        @Override
        public void visit(TimestampValue expression) {
            result = FACTORY.getConstantLiteral(expression.getValue().toString(), Predicate.COL_TYPE.DATETIME);
        }

        /*
            BINARY OPERATIONS
        */

        @Override
        public void visit(Addition addition) {
            visitBinaryExpression(addition,
                    (t1, t2) -> FACTORY.getFunction(ExpressionOperation.ADD, t1, t2));
        }

        @Override
        public void visit(Subtraction subtraction) {
            visitBinaryExpression(subtraction,
                    (t1, t2) -> FACTORY.getFunction(ExpressionOperation.SUBTRACT, t1, t2));
        }

        @Override
        public void visit(Multiplication multiplication) {
            visitBinaryExpression(multiplication,
                    (t1, t2) -> FACTORY.getFunction(ExpressionOperation.MULTIPLY, t1, t2));
        }

        @Override
        public void visit(Division division) {
            visitBinaryExpression(division,
                    (t1, t2) -> FACTORY.getFunction(ExpressionOperation.DIVIDE, t1, t2));
        }

        // TODO: introduce operation and implement
        @Override
        public void visit(Modulo modulo) {
            throw new UnsupportedSelectQueryException("Not supported yet", modulo);
        }

        @Override
        public void visit(Concat concat) {
            visitBinaryExpression(concat,
                    (t1, t2) -> FACTORY.getFunction(ExpressionOperation.CONCAT, t1, t2));
        }



        @Override
        public void visit(EqualsTo expression) {
            visitBinaryExpression(expression,
                    (t1, t2) -> FACTORY.getFunction(ExpressionOperation.EQ, t1, t2));
        }

        @Override
        public void visit(GreaterThan expression) {
            visitBinaryExpression(expression,
                    (t1, t2) -> FACTORY.getFunction(ExpressionOperation.GT, t1, t2));
        }

        @Override
        public void visit(GreaterThanEquals expression) {
            visitBinaryExpression(expression,
                    (t1, t2) -> FACTORY.getFunction(ExpressionOperation.GTE, t1, t2));
        }

        @Override
        public void visit(MinorThan minorThan) {
            visitBinaryExpression(minorThan,
                    (t1, t2) -> FACTORY.getFunction(ExpressionOperation.LT, t1, t2));
        }

        @Override
        public void visit(MinorThanEquals minorThanEquals) {
            visitBinaryExpression(minorThanEquals,
                    (t1, t2) -> FACTORY.getFunction(ExpressionOperation.LTE, t1, t2));
        }

        @Override
        public void visit(NotEqualsTo notEqualsTo) {
            visitBinaryExpression(notEqualsTo,
                    (t1, t2) -> FACTORY.getFunction(ExpressionOperation.NEQ, t1, t2));
        }



        @Override
        public void visit(LikeExpression likeExpression) {
            visitBinaryExpression(likeExpression, (t1, t2) -> FACTORY.getSQLFunctionLike(t1, t2));
        }

        @Override
        public void visit(RegExpMySQLOperator regExpMySQLOperator) {
            Term flags;
            switch (regExpMySQLOperator.getOperatorType()) {
                case MATCH_CASESENSITIVE:
                    flags = FACTORY.getConstantLiteral("");
                    break;
                case MATCH_CASEINSENSITIVE:
                    flags = FACTORY.getConstantLiteral("i");
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown operator: " + regExpMySQLOperator);
            }
            visitBinaryExpression(regExpMySQLOperator,
                    (t1, t2) ->  FACTORY.getFunction(ExpressionOperation.REGEX, t1, t2, flags));
        }

        @Override
        public void visit(RegExpMatchOperator expression) {
            Term flags;
            boolean not;
            switch (expression.getOperatorType()) {
                case MATCH_CASESENSITIVE:
                    flags = FACTORY.getConstantLiteral("");
                    not = false;
                    break;
                case MATCH_CASEINSENSITIVE:
                    flags = FACTORY.getConstantLiteral("i");
                    not = false;
                    break;
                case NOT_MATCH_CASESENSITIVE:
                    flags = FACTORY.getConstantLiteral("");
                    not = true;
                    break;
                case NOT_MATCH_CASEINSENSITIVE:
                    flags = FACTORY.getConstantLiteral("i");
                    not = true;
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown operator: " + expression);
            }
            visitBinaryExpression(expression, (t1, t2) ->  not
                    ? FACTORY.getFunctionNOT(FACTORY.getFunction(ExpressionOperation.REGEX, t1, t2, flags))
                    : FACTORY.getFunction(ExpressionOperation.REGEX, t1, t2, flags));
        }



        @Override
        public void visit(AndExpression andExpression) {
            visitBinaryExpression(andExpression, (t1, t2) -> FACTORY.getFunctionAND(t1, t2));
        }

        @Override
        public void visit(OrExpression orExpression) {
            visitBinaryExpression(orExpression, (t1, t2) -> FACTORY.getFunctionOR(t1, t2));
        }



        @Override
        public void visit(Between expression) {
            Term t1 = translate(expression.getLeftExpression());
            Term t2 = translate(expression.getBetweenExpressionStart());
            Term atom1 = FACTORY.getFunction(ExpressionOperation.GTE, t1, t2);

            Term t3 = translate(expression.getLeftExpression());
            Term t4 = translate(expression.getBetweenExpressionEnd());
            Term atom2 = FACTORY.getFunction(ExpressionOperation.LTE, t3, t4);

            result = FACTORY.getFunctionAND(atom1, atom2);
        }


        @Override
        public void visit(InExpression expression) {

            // TODO: replace with "native" FACTORY.get.. calls

            Expression left = expression.getLeftExpression();
            // rightItemsList can be SubSelect, ExpressionList and MultiExpressionList
            ExpressionList rightItemsList = (ExpressionList) expression.getRightItemsList();
            if (rightItemsList == null)
                throw new UnsupportedOperationException();

            final ImmutableList.Builder<EqualsTo> builderEqualsToList = new ImmutableList.Builder<>();
            rightItemsList.getExpressions().forEach( item -> {
                final EqualsTo eq = new EqualsTo();
                eq.setLeftExpression(left);
                eq.setRightExpression(item);
                builderEqualsToList.add(eq);
            });
            ImmutableList<EqualsTo> equalsToList = builderEqualsToList.build();
            int size = equalsToList.size();
            if (size > 1) {
                OrExpression or = new OrExpression(equalsToList.get(size - 1), equalsToList.get(size - 2));

                for (int i = size - 3; i >= 0; i--)
                    or = new OrExpression(equalsToList.get(i), or);

                result = translate(or);
            } else {
                result = translate(equalsToList.get(0));
            }
        }

        /*
                UNARY OPERATIONS
         */

        @Override
        public void visit(IsNullExpression expression) {
            visitUnaryExpression(expression.getLeftExpression(), expression.isNot(),
                    t -> FACTORY.getFunctionIsNull(t));
        }

        @Override
        public void visit(Parenthesis expression) {
            visitUnaryExpression(expression.getExpression(), expression.isNot(),
                    UnaryOperator.identity());
        }

        @Override
        public void visit(SignedExpression signedExpression) {
            UnaryOperator<Term> op;
            switch (signedExpression.getSign()) {
                case '-' :
                    op = t -> FACTORY.getFunction(ExpressionOperation.MINUS, t);
                    break;
                case '+':
                    op = UnaryOperator.identity();
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown operator: " + signedExpression);
            }
            visitUnaryExpression(signedExpression.getExpression(), false, op);
        }



        @Override
        public void visit(Column expression) {

            QuotedID column = idfac.createAttributeID(expression.getColumnName());
            RelationID relation = null;
            if (expression.getTable().getName() != null)
                relation = idfac.createRelationID(expression.getTable().getSchemaName(), expression.getTable().getName());

            QualifiedAttributeID qa = new QualifiedAttributeID(relation, column);

            Term term = attributes.get(qa);

            if (term != null) {
                /*
                 * If the termName is not null, create a variable
                 */
                result = term;
            }
            else {
                // TODO: careful here
                // Constructs constant
                // if the columns contains a boolean value
                String columnName = expression.getColumnName();
                // check whether it is an SQL boolean value
                String lowerCase = columnName.toLowerCase();
                if (lowerCase.equals("true")) {
                    result = FACTORY.getBooleanConstant(true);
                }
                else if (lowerCase.equals("false")) {
                    result = FACTORY.getBooleanConstant(false);
                }
                else
                    throw new RuntimeException( "Unable to find column name for variable: "
                            + columnName);
            }

        }



        @Override
        // TODO: this should be supported
        // Syntax:
        //      * CASE
        //      * WHEN condition THEN expression
        //      * [WHEN condition THEN expression]...
        //      * [ELSE expression]
        //      * END
        // or
        //      * CASE expression
        //      * WHEN condition THEN expression
        //      * [WHEN condition THEN expression]...
        //      * [ELSE expression]
        //      * END
        public void visit(CaseExpression caseExpression) {
            throw new UnsupportedOperationException();
        }

        @Override
        // TODO: this should be supported
        public void visit(WhenClause whenClause) {
            throw new UnsupportedOperationException();
        }


        @Override
        public void visit(CastExpression expression) {
            // TODO
            Expression column = expression.getLeftExpression();
            String columnName = column.toString();
            //    String variableName = attributes.lookup(columnName);
            //    if (variableName == null) {
            //        throw new RuntimeException(
            //                "Unable to find column name for variable: " + columnName);
            //    }
            //    Term var = FACTORY.getVariable(variableName);

            //     ColDataType datatype = expression.getType();



            //    Term var2 = null;

            //first value is a column, second value is a datatype. It can  also have the size

            //    result = FACTORY.getFunctionCast(var, var2);

        }



        @Override
        public void visit(SubSelect subSelect) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(ExistsExpression existsExpression) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(AllComparisonExpression allComparisonExpression) { throw new UnsupportedOperationException(); }

        @Override
        public void visit(AnyComparisonExpression anyComparisonExpression) { throw new UnsupportedOperationException(); }




        @Override
        public void visit(BitwiseAnd bitwiseAnd) {
            throw new UnsupportedSelectQueryException("Bitwise AND not supported", bitwiseAnd);
        }

        @Override
        public void visit(BitwiseOr bitwiseOr) {
            throw new UnsupportedSelectQueryException("Bitwise OR not supported", bitwiseOr);
        }

        @Override
        public void visit(BitwiseXor bitwiseXor) {
            throw new UnsupportedSelectQueryException("Bitwise XOR not supported", bitwiseXor);
        }

        @Override
        public void visit(AnalyticExpression expression) {
            throw new UnsupportedSelectQueryException("Analytic expressions not supported", expression);
        }

        // TODO: check
        @Override
        public void visit(ExtractExpression expression) {
            throw new UnsupportedOperationException();
        }

        // TODO: check
        @Override
        public void visit(IntervalExpression expression) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void visit(OracleHierarchicalExpression expression) {
            throw new UnsupportedSelectQueryException("Oracle hierarchical expressions not supported", expression);
        }

        @Override
        public void visit(Matches matches) {
            throw new UnsupportedSelectQueryException("Oracle join syntax not supported", matches);
        }

        @Override
        public void visit(JsonExpression jsonExpr) {
            throw new InvalidSelectQueryEcxeption("JSON expressions are not allowed", jsonExpr);
        }

        @Override
        public void visit(JdbcParameter jdbcParameter) {
            throw new InvalidSelectQueryEcxeption("JDBC parameters are not allowed", jdbcParameter);
        }

        @Override
        public void visit(JdbcNamedParameter jdbcNamedParameter) {
            throw new InvalidSelectQueryEcxeption("JDBC named parameters are not allowed", jdbcNamedParameter);
        }
    }
}