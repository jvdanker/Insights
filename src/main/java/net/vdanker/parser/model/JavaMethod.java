package net.vdanker.parser.model;

import org.eclipse.jgit.lib.ObjectId;

import java.util.List;

public record JavaMethod(
        ObjectId objectId,
        String packageName,
        String className,
        String methodName,
        String fqName,
        List<FormalParameter> formalParametersList,
        int blockStatements,
        List<String> methodCalls,
        int complexity,
        int localVariableDeclarations,
        int methodStart,
        int methodEnd) {
}
