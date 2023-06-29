package net.vdanker.parser.model;

import java.util.List;

public record JavaStats(
        String fqClassName,
        String packageDeclaration,
        List<JavaImportDeclaration> imports, List<JavaMethod> methods) {
}
