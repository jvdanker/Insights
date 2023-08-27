package net.vdanker.parser;

import org.antlr.v4.runtime.tree.ParseTreeListener;

public class JavaClassListener extends parsers.JavaParserBaseListener implements ParseTreeListener {

    public void enterMethodDeclaration(parsers.JavaParser.MethodDeclarationContext ctx) {
        System.out.println(ctx);
    }
}
