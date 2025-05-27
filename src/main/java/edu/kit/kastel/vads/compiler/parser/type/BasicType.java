package edu.kit.kastel.vads.compiler.parser.type;

import java.util.Locale;

import edu.kit.kastel.vads.compiler.lexer.KeywordType;

public enum BasicType implements Type {
    INT(KeywordType.INT),
    BOOL(KeywordType.BOOL);

    private final KeywordType keywordType;

    BasicType(KeywordType kw) {
        this.keywordType = kw;
    }

    public KeywordType getKeywordType() {
        return keywordType;
    }

    @Override
    public String asString() {
        return name().toLowerCase(Locale.ROOT);
    }
}
