package edu.kit.kastel.vads.compiler.lexer;

import java.util.EnumSet;

import edu.kit.kastel.vads.compiler.Span;

public record Operator(OperatorType type, Span span) implements Token {

    @Override
    public boolean isOperator(OperatorType operatorType) {
        return type() == operatorType;
    }

    @Override
    public String asString() {
        return type().toString();
    }

    public enum OperatorType {
        
        LOGICAL_NOT("!", 0),
        BITWISE_NOT("~", 0),

        MUL("*", 1),
        DIV("/", 1),
        MOD("%", 1),

        MINUS("-",2),
        PLUS("+", 2),

        LSHIFT("<<", 3),
        RSHIFT(">>", 3),

        LOGICAL_GT(">", 4),
        LOGICAL_GT_OR_EQUAL(">=", 4),
        LOGICAL_LT("<", 4),
        LOGICAL_LT_OR_EQUAL("<=", 4),

        LOGICAL_EQUAL("==", 5),
        LOGICAL_UNEQUAL("!=", 5),

        BITWISE_AND("&", 6),

        BITWISE_XOR("^", 7),

        BITWISE_OR("|", 8),
        
        LOGICAL_AND("&&", 9),

        LOGICAL_OR("||", 10),

        TERNIARY_QUESTION("?", -1),
        TERNIARY_COLON(":", -1),

        ASSIGN("=", -1),
        ASSIGN_PLUS("+=", -1),
        ASSIGN_MINUS("-=", -1),
        ASSIGN_MUL("*=", -1),
        ASSIGN_DIV("/=",-1),
        ASSIGN_MOD("%=",-1),
        ASSIGN_AND("&=",-1),
        ASSIGN_XOR("^=", -1),
        ASSIGN_OR("|=", -1),
        ASSIGN_LSHIFT("<<=", -1),
        ASSIGN_RSHIFT(">>=", -1),
        ;

        private final String value;
        private final int precedence;

        OperatorType(String value, int precedence) {
            this.value = value;
            this.precedence = precedence;
        }

        public int getPrecedence() {
            return precedence;
        }

        public static int maxPrecedence() {
            return EnumSet.allOf(OperatorType.class).stream()
                    .map(OperatorType::getPrecedence)
                    .max(Integer::compareTo)
                    .get();
        }

        @Override
        public String toString() {
            return this.value;
        }
    }
}
