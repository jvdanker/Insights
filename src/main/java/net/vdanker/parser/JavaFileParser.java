package net.vdanker.parser;

import net.vdanker.parser.model.JavaStats;
import parsers.JavaLexer;
import parsers.JavaParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.io.InputStream;

public class JavaFileParser {
    public static JavaStats parse(InputStream is) throws IOException {
        var charStream = CharStreams.fromStream(is);

        var lexer = new JavaLexer(charStream);
        var tokens = new CommonTokenStream(lexer);
        var parser = new JavaParser(tokens);

        var tree = parser.compilationUnit();
        var listener = new JavaListener(parser);

        var walker = new ParseTreeWalker();
        walker.walk(listener, tree);

        return listener.getStats();
    }
}