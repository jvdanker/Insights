package net.vdanker.mappers;

import java_parser.JavaParser;
import net.vdanker.parser.Pair;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;

public class TestReducer implements java.util.function.BinaryOperator<net.vdanker.parser.Pair<java_parser.JavaParser, java_parser.JavaParser.CompilationUnitContext>> {
    @Override
    public Pair<JavaParser, JavaParser.CompilationUnitContext> apply(
            Pair<JavaParser, JavaParser.CompilationUnitContext> accumulator,
            Pair<JavaParser, JavaParser.CompilationUnitContext> element) {
        if (accumulator.getKey() == null) {
            accumulator.setKey(element.getKey());
            JavaParser.CompilationUnitContext context = new JavaParser.CompilationUnitContext(null, 0);
            context.children = new ArrayList<>();
            context.children.add(element.getValue());
            accumulator.setValue(context);
        } else {
            accumulator.getValue().children.add(element.getValue());
        }

        return accumulator;
    }
}
