package edu.kit.kastel.vads.compiler.parser;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.lexer.Identifier;
import edu.kit.kastel.vads.compiler.lexer.Keyword;
import edu.kit.kastel.vads.compiler.lexer.KeywordType;
import edu.kit.kastel.vads.compiler.lexer.NumberLiteral;
import edu.kit.kastel.vads.compiler.lexer.Operator;
import edu.kit.kastel.vads.compiler.lexer.Operator.OperatorType;
import edu.kit.kastel.vads.compiler.lexer.Separator;
import edu.kit.kastel.vads.compiler.lexer.Separator.SeparatorType;
import edu.kit.kastel.vads.compiler.lexer.Token;
import edu.kit.kastel.vads.compiler.parser.ast.AssignmentTree;
import edu.kit.kastel.vads.compiler.parser.ast.BinaryOperationTree;
import edu.kit.kastel.vads.compiler.parser.ast.BitwiseNegateTree;
import edu.kit.kastel.vads.compiler.parser.ast.BlockTree;
import edu.kit.kastel.vads.compiler.parser.ast.BoolLiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.BreakTree;
import edu.kit.kastel.vads.compiler.parser.ast.ContinueTree;
import edu.kit.kastel.vads.compiler.parser.ast.DeclarationTree;
import edu.kit.kastel.vads.compiler.parser.ast.ExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.ForTree;
import edu.kit.kastel.vads.compiler.parser.ast.FunctionTree;
import edu.kit.kastel.vads.compiler.parser.ast.IdentExpressionTree;
import edu.kit.kastel.vads.compiler.parser.ast.IfTree;
import edu.kit.kastel.vads.compiler.parser.ast.IntLiteralTree;
import edu.kit.kastel.vads.compiler.parser.ast.LValueIdentTree;
import edu.kit.kastel.vads.compiler.parser.ast.LValueTree;
import edu.kit.kastel.vads.compiler.parser.ast.LogicalNegateTree;
import edu.kit.kastel.vads.compiler.parser.ast.NameTree;
import edu.kit.kastel.vads.compiler.parser.ast.NegateTree;
import edu.kit.kastel.vads.compiler.parser.ast.ProgramTree;
import edu.kit.kastel.vads.compiler.parser.ast.ReturnTree;
import edu.kit.kastel.vads.compiler.parser.ast.StatementTree;
import edu.kit.kastel.vads.compiler.parser.ast.TernaryTree;
import edu.kit.kastel.vads.compiler.parser.ast.TypeTree;
import edu.kit.kastel.vads.compiler.parser.ast.WhileTree;
import edu.kit.kastel.vads.compiler.parser.symbol.Name;
import edu.kit.kastel.vads.compiler.parser.type.BasicType;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class Parser {
    private final TokenSource tokenSource;

    public Parser(TokenSource tokenSource) {
        this.tokenSource = tokenSource;
    }

    public ProgramTree parseProgram() {
        ProgramTree programTree = new ProgramTree(List.of(parseFunction()));
        if (this.tokenSource.hasMore()) {
            throw new ParseException("expected end of input but got " + this.tokenSource.peek());
        }
        return programTree;
    }

    private FunctionTree parseFunction() {
        Keyword returnType = this.tokenSource.expectKeyword(KeywordType.INT);
        Identifier identifier = this.tokenSource.expectIdentifier();
        if (!identifier.value().equals("main")) {
            throw new ParseException("expected main function but got " + identifier);
        }
        this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
        this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
        BlockTree body = parseBlock();
        return new FunctionTree(
            new TypeTree(BasicType.INT, returnType.span()),
            name(identifier),
            body
        );
    }

    private BlockTree parseBlock() {
        Separator bodyOpen = this.tokenSource.expectSeparator(SeparatorType.BRACE_OPEN);
        List<StatementTree> statements = new ArrayList<>();
        while (!(this.tokenSource.peek() instanceof Separator sep && sep.type() == SeparatorType.BRACE_CLOSE)) {
            statements.add(parseStatement());
        }
        Separator bodyClose = this.tokenSource.expectSeparator(SeparatorType.BRACE_CLOSE);
        return new BlockTree(statements, bodyOpen.span().merge(bodyClose.span()));
    }

    private StatementTree parseStatement() {
        StatementTree statement;
        List<KeywordType> types = EnumSet.allOf(BasicType.class).stream().map(BasicType::getKeywordType).toList();

        // If type is found -> declaration
        if (types.stream().anyMatch(e -> this.tokenSource.peek().isKeyword(e))) {
            statement = parseDeclaration();
        } else if (this.tokenSource.peek().isKeyword(KeywordType.RETURN)) {
            statement = parseReturn();

        } else if (this.tokenSource.peek().isSeparator(SeparatorType.BRACE_OPEN)) {
            return parseBlock();
        }
        else if(this.tokenSource.peek().isKeyword(KeywordType.BREAK)) {
            Keyword word = this.tokenSource.expectKeyword(KeywordType.BREAK);
            this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);
            return new BreakTree(word.span());
        } else if(this.tokenSource.peek().isKeyword(KeywordType.CONTINUE)) {
            Keyword word = this.tokenSource.expectKeyword(KeywordType.CONTINUE);
            this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);
            return new ContinueTree(word.span());
        }
        else if (this.tokenSource.peek().isKeyword(KeywordType.IF)) {
            return parseIf();
        }
        else if (this.tokenSource.peek().isKeyword(KeywordType.WHILE)) {
            return parseWhile();
        }
        else if (this.tokenSource.peek().isKeyword(KeywordType.FOR)) {
            return parseFor();
        } else {
            statement = parseSimple();
        }

        this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);
        return statement;
    }

    private StatementTree parseFor() {
        Keyword word = this.tokenSource.expectKeyword(KeywordType.FOR);
        Optional<StatementTree> definition = Optional.empty();
        Optional<StatementTree> statement = Optional.empty();
        this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
        if (!tokenSource.peek().isSeparator(SeparatorType.SEMICOLON)) {
            if (this.tokenSource.peek().isType()) {
                definition = Optional.of(parseDeclaration());
            } else {
                definition = Optional.of(parseSimple());
            }
        }
        this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);
        ExpressionTree condition = this.parseExpression();
        this.tokenSource.expectSeparator(SeparatorType.SEMICOLON);
        //TODO: Changeback
        if (!tokenSource.peek().isSeparator(SeparatorType.SEMICOLON)) {
            if (this.tokenSource.peek().isType()) {
                statement = Optional.of(parseDeclaration());
            } else {
                statement = Optional.of(parseSimple());
            }
        }

        this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);

        StatementTree body = parseStatement();

        return new ForTree(definition, condition, statement, body, word.span().start());
    }

    private StatementTree parseWhile(){
        Keyword word = this.tokenSource.expectKeyword(KeywordType.WHILE);
        this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
        ExpressionTree condition = parseExpression();
        this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
        StatementTree body = parseStatement();

        return new WhileTree(condition, body, word.span().start());
    }

    private StatementTree parseIf() {
        Keyword word = this.tokenSource.expectKeyword(KeywordType.IF);
        this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
        ExpressionTree e = parseExpression();
        this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
        StatementTree then = parseStatement();
        Optional<StatementTree> orElse = Optional.empty();

        if (this.tokenSource.peek().isKeyword(KeywordType.ELSE)) {
            this.tokenSource.expectKeyword(KeywordType.ELSE);
            orElse = Optional.of(parseStatement());
        }

        return new IfTree(e, then, orElse, word.span().start());
    }

    private StatementTree parseDeclaration() {
        Keyword type = this.tokenSource.expectType();
        Identifier ident = this.tokenSource.expectIdentifier();
        ExpressionTree expr = null;
        if (this.tokenSource.peek().isOperator(OperatorType.ASSIGN)) {
            this.tokenSource.expectOperator(OperatorType.ASSIGN);
            expr = parseExpression();
        }
        return new DeclarationTree(new TypeTree(this.toType(type), type.span()), name(ident), expr);
    }

    private StatementTree parseSimple() {
        LValueTree lValue = parseLValue();
        Operator assignmentOperator = parseAssignmentOperator();
        ExpressionTree expression = parseExpression();
        return new AssignmentTree(lValue, assignmentOperator, expression);
    }

    private Operator parseAssignmentOperator() {
        if (this.tokenSource.peek() instanceof Operator op) {
            return switch (op.type()) {
                case ASSIGN,
                        ASSIGN_DIV,
                        ASSIGN_MINUS,
                        ASSIGN_MOD,
                        ASSIGN_MUL,
                        ASSIGN_PLUS,
                        ASSIGN_XOR,
                        ASSIGN_AND,
                        ASSIGN_LSHIFT,
                        ASSIGN_RSHIFT,
                        ASSIGN_OR -> {
                    this.tokenSource.consume();
                    yield op;
                }
                default -> throw new ParseException("expected assignment but got " + op.type());
            };
        }
        throw new ParseException("expected assignment but got " + this.tokenSource.peek());
    }

    private LValueTree parseLValue() {
        if (this.tokenSource.peek().isSeparator(SeparatorType.PAREN_OPEN)) {
            this.tokenSource.expectSeparator(SeparatorType.PAREN_OPEN);
            LValueTree inner = parseLValue();
            this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
            return inner;
        }
        Identifier identifier = this.tokenSource.expectIdentifier();
        return new LValueIdentTree(name(identifier));
    }

    private StatementTree parseReturn() {
        Keyword ret = this.tokenSource.expectKeyword(KeywordType.RETURN);
        ExpressionTree expression = parseExpression();
        return new ReturnTree(expression, ret.span().start());
    }

    private ExpressionTree parseExpression() {
        ExpressionTree lhs = parseExpressionWithPrecedence(OperatorType.maxPrecedence());

        //Found ternary
        if (this.tokenSource.peek() instanceof Operator op && op.type() == OperatorType.TERNARY_QUESTION) {
            this.tokenSource.consume();

            ExpressionTree trueBranch = parseExpression();
            tokenSource.expectOperator(OperatorType.TERNARY_COLON);
            ExpressionTree falseBranch = parseExpression();

            return new TernaryTree(lhs, trueBranch, falseBranch);
        }

        return lhs;
    }

    private ExpressionTree parseExpressionWithPrecedence(int precedence) {
        ExpressionTree lhs = precedence == 0 ? parseFactor() : parseExpressionWithPrecedence(precedence - 1);

        while (true) {
            if (this.tokenSource.peek() instanceof Operator(OperatorType type, _)
              && (type.getPrecedence() == precedence)) {
                this.tokenSource.consume();
                ExpressionTree rhs = precedence == 0 ? parseFactor() : parseExpressionWithPrecedence(precedence - 1);
                lhs = new BinaryOperationTree(lhs, rhs, type);
            }
            else {
                return lhs;
            }
        }
    }

    private ExpressionTree parseFactor() {
        return switch (this.tokenSource.peek()) {
            case Separator(var type, _) when type == SeparatorType.PAREN_OPEN -> {
                this.tokenSource.consume();
                ExpressionTree expression = parseExpression();
                this.tokenSource.expectSeparator(SeparatorType.PAREN_CLOSE);
                yield expression;
            }
            case Operator(var type, _) when type == OperatorType.MINUS -> {
                Span span = this.tokenSource.consume().span();
                yield new NegateTree(parseFactor(), span);
            }
            case Operator(var type, _) when type == OperatorType.LOGICAL_NOT -> {
                Span span = this.tokenSource.consume().span();
                yield new LogicalNegateTree(parseFactor(), span);
            }
            case Operator(var type, _) when type == OperatorType.BITWISE_NOT -> {
                Span span = this.tokenSource.consume().span();
                yield new BitwiseNegateTree(parseFactor(), span);
            }
            case Identifier ident -> {
                this.tokenSource.consume();
                yield new IdentExpressionTree(name(ident));
            }
            case Keyword(KeywordType type, Span span) -> {
                if (type == KeywordType.TRUE) {
                    this.tokenSource.consume();
                    yield new BoolLiteralTree(true, span);
                }

                if (type == KeywordType.FALSE) {
                    this.tokenSource.consume();
                    yield new BoolLiteralTree(false, span);
                }

                throw new ParseException("keyword " + type + " is not allowed here");
            }
            case NumberLiteral(String value, int base, Span span) -> {
                this.tokenSource.consume();
                yield new IntLiteralTree(value, base, span);
            }
            case Token t -> throw new ParseException("invalid factor " + t);
        };
    }

    private static NameTree name(Identifier ident) {
        return new NameTree(Name.forIdentifier(ident), ident.span());
    }

    private BasicType toType(Keyword word) {
        Map<KeywordType, BasicType> mapped = EnumSet.allOf(BasicType.class).stream()
                                                    .collect(Collectors
                                                            .toMap(e -> e.getKeywordType(), e -> e));

        return mapped.get(word.type());
    }
}
