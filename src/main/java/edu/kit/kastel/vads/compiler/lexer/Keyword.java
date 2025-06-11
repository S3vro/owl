package edu.kit.kastel.vads.compiler.lexer;

import edu.kit.kastel.vads.compiler.Span;
import edu.kit.kastel.vads.compiler.parser.type.BasicType;
import java.util.EnumSet;
import java.util.Map;
import java.util.stream.Collectors;

public record Keyword(KeywordType type, Span span) implements Token {
    @Override
    public boolean isKeyword(KeywordType keywordType) {
        return type() == keywordType;
    }

    public boolean isType() {
        Map<KeywordType, BasicType> mapped = EnumSet.allOf(BasicType.class).stream()
          .collect(Collectors
            .toMap(e -> e.getKeywordType(), e -> e));
        return mapped.containsKey(type);
    }

    @Override
    public String asString() {
        return type().keyword();
    }
}
