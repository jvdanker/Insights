package net.vdanker.mappers;

import java_parser.JavaParser;
import net.vdanker.parser.Pair;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;

import java.util.ArrayList;
import java.util.List;

public class TreeMerger implements  java.util.function.BinaryOperator<Pair<Parser, ParserRuleContext>> {

    @Override
    public Pair<Parser, ParserRuleContext> apply(
            Pair<Parser, ParserRuleContext> c1,
            Pair<Parser, ParserRuleContext> c2) {
        return null;
    }
}
