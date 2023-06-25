package net.vdanker.parser;

import java_parser.JavaParser;
import net.vdanker.parser.model.JavaMethod;
import net.vdanker.parser.model.JavaStats;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenStream;

import java.util.ArrayList;
import java.util.List;

public class JavaListener extends java_parser.JavaParserBaseListener {
    private final Parser parser;
    private String packageName;
    private String className;

    List<JavaMethod> methods = new ArrayList<>();
    private List<String> methodCalls;

    List<String> imports = new ArrayList<>();
    private int blockStatements;

    public JavaListener(Parser parser) {
        this.parser = parser;
    }

    JavaStats getStats() {
        return new JavaStats(
                this.imports,
                this.methods
        );
    }

    public void enterPackageDeclaration(JavaParser.PackageDeclarationContext ctx) {
        this.packageName = ctx.qualifiedName().getText();
    }

    public void enterImportDeclaration(JavaParser.ImportDeclarationContext ctx) {
        this.imports.add(ctx.qualifiedName().getText());
    }

    public void enterClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
        this.className = ctx.identifier().getText();
    }

    public void enterMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
        this.blockStatements = 0;
        this.methodCalls = new ArrayList<>();
    }

    public void exitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
        var method = new JavaMethod(
                ctx.identifier().getText(),
                this.blockStatements,
                this.methodCalls);
        this.methods.add(method);

        this.methodCalls = null;
    }

    public void enterMethodCall(JavaParser.MethodCallContext ctx) {
        if (this.methodCalls != null) {
            TokenStream tokens = parser.getTokenStream();
            var x = tokens.getText(ctx.getParent().children.get(0).getSourceInterval());

            this.methodCalls.add(String.format("ON %s -> %s", x, ctx.getText()));
        }
    }

    public void enterBlockStatement(JavaParser.BlockStatementContext ctx) {
        this.blockStatements++;
    }

    public String getFQClassName() {
        if (this.packageName != null) {
            return String.format("%s.%s", this.packageName, this.className);
        }

        return this.className;
    }
}
