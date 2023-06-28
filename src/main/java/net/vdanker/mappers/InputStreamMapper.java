package net.vdanker.mappers;

import java_parser.JavaParser;
import net.vdanker.parser.JavaFileParser;
import net.vdanker.parser.Pair;
import net.vdanker.parser.model.JavaStats;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Stream;

public class InputStreamMapper {
    public static Pair<String, JavaStats> toJavaStats(InputStream is) {
        try {
            return JavaFileParser.parse(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static JavaParser.CompilationUnitContext toTree(InputStream is) {
        try {
            CharStream charStream = CharStreams.fromStream(is);
            var lexer = new java_parser.JavaLexer(charStream);
            var tokens = new CommonTokenStream(lexer);
            var parser = new java_parser.JavaParser(tokens);

            return parser.compilationUnit();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static JavaParser toParser(InputStream is) {
        try {
            CharStream charStream = CharStreams.fromStream(is);
            var lexer = new java_parser.JavaLexer(charStream);
            var tokens = new CommonTokenStream(lexer);
            return new java_parser.JavaParser(tokens);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Token> toTokenEmitter(InputStream is) {
        try {
            CharStream charStream = CharStreams.fromStream(is);
            var lexer = new java_parser.JavaLexer(charStream);
            var tokens = new CommonTokenStream(lexer);
            tokens.fill();

            return tokens.getTokens();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
