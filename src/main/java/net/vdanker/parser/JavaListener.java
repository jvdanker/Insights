package net.vdanker.parser;

import org.eclipse.jgit.lib.ObjectId;
import parsers.JavaParser;
import net.vdanker.parser.model.FormalParameter;
import net.vdanker.parser.model.JavaImportDeclaration;
import net.vdanker.parser.model.JavaMethod;
import net.vdanker.parser.model.JavaStats;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTreeListener;

import java.util.ArrayList;
import java.util.List;

public class JavaListener extends parsers.JavaParserBaseListener implements ParseTreeListener {
    final Parser parser;
    private final ObjectId objectId;
    String packageDeclaration;
    String className;

    List<JavaMethod> methods = new ArrayList<>();
    List<String> methodCalls;

    List<JavaImportDeclaration> importDeclarations = new ArrayList<>();
    int blockStatements;
    private List<FormalParameter> formalParametersList;
    private int complexity;
    private int localVariableDeclarations;
    private int methodStart;

    public JavaListener(ObjectId objectId, Parser parser) {
        this.parser = parser;
        this.objectId = objectId;
    }

    JavaStats getStats() {
        return new JavaStats(
                this.getFQClassName(),
                this.packageDeclaration,
                this.importDeclarations,
                this.methods
        );
    }

    public void enterPackageDeclaration(JavaParser.PackageDeclarationContext ctx) {
        this.packageDeclaration = ctx.qualifiedName().getText();
    }

    public void enterImportDeclaration(JavaParser.ImportDeclarationContext ctx) {
        this.importDeclarations.add(
                new JavaImportDeclaration(
                        ctx.qualifiedName().getChild(ctx.qualifiedName().getChildCount() - 1).getText(),
                        ctx.qualifiedName().getText()));
    }

    public void enterClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
        this.className = ctx.identifier().getText();
    }

    public void enterMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
        this.blockStatements = 0;
        this.complexity = 1;
        this.localVariableDeclarations = 0;
        this.methodStart = ctx.start.getLine();
        this.methodCalls = new ArrayList<>();
    }

    public void exitMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
        String identifier = ctx.identifier().getText();
        var method = new JavaMethod(
                this.objectId,
                this.packageDeclaration,
                this.className,
                identifier,
                String.format("%s.%s.%s", this.packageDeclaration, this.className, identifier),
                this.formalParametersList,
                this.blockStatements,
                this.methodCalls,
                this.complexity,
                this.localVariableDeclarations,
                this.methodStart,
                ctx.stop.getLine());
        this.methods.add(method);

        this.methodCalls = null;
        this.formalParametersList = null;
    }

    public void enterFormalParameterList(JavaParser.FormalParameterListContext ctx) {
        this.formalParametersList = new ArrayList<>();
    }

    public void enterFormalParameter(JavaParser.FormalParameterContext ctx) {
        this.formalParametersList.add(new FormalParameter(
                ctx.typeType().getText(),
                ctx.variableDeclaratorId().getText()
        ));
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

    public void enterParExpression(JavaParser.ParExpressionContext ctx) {
        this.complexity++;
    }

    public void enterLocalVariableDeclaration(JavaParser.LocalVariableDeclarationContext ctx) {
        this.localVariableDeclarations++;
    }

    private String getFQClassName() {
        if (this.packageDeclaration != null) {
            return String.format("%s.%s", this.packageDeclaration, this.className);
        }

        return this.className;
    }
}
