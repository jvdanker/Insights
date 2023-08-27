package net.vdanker.parser;

import net.vdanker.parser.model.*;
import org.eclipse.jgit.lib.ObjectId;
import parsers.JavaParser;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ParseTreeListener;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

public class JavaListener extends parsers.JavaParserBaseListener implements ParseTreeListener {
    final Parser parser;
    final ObjectId objectId;
    String packageDeclaration;
    String className;
//    List<JavaMethod> methods = new ArrayList<>();
    List<JavaClass> classes = new ArrayList<>();
    Stack<JavaClass> currentClass = new Stack<>();
    List<String> methodCalls;
    List<JavaImportDeclaration> importDeclarations = new ArrayList<>();
    int blockStatements;
    List<FormalParameter> formalParametersList;
    int complexity;
    int localVariableDeclarations;
    int methodStart;

    public JavaListener(ObjectId objectId, Parser parser) {
        this.parser = parser;
        this.objectId = objectId;
    }

    JavaStats getStats() {
        return new JavaStats(
                this.getFQClassName(),
                this.packageDeclaration,
                this.importDeclarations,
//                this.methods,
                this.classes
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

        this.currentClass.push(
                new JavaClass(
                        this.getFQClassName(),
                        new ArrayList<>()));
    }

    public void exitClassDeclaration(JavaParser.ClassDeclarationContext ctx) {
        JavaClass pop = this.currentClass.pop();
        this.classes.add(pop);
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
        if (this.currentClass.isEmpty()) return; // enums
        this.currentClass.peek().methods().add(method);

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
