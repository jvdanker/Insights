package net.vdanker.parser.model;

import java.util.List;

public record JavaMethod(
        String fqName,
        String name,
        List<FormalParameter> formalParametersList,
        int blockStatements,
        List<String> methodCalls,
        int complexity,
        int localVariableDeclarations) {
}
