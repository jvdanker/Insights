package net.vdanker.parser.model;

import java.util.List;

public record JavaStats(String packageDeclaration, List<JavaImportDeclaration> imports, List<JavaMethod> methods) {
}
