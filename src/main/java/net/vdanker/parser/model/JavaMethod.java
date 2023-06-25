package net.vdanker.parser.model;

import java.util.List;

public record JavaMethod(
        String name,
        int blockStatements,
        List<String> methodCalls) {
}
