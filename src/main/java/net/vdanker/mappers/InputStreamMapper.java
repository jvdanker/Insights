package net.vdanker.mappers;

import parsers.JavaParser;
import parsers.XMLParser;
import net.vdanker.parser.JavaFileParser;
import net.vdanker.parser.XmlFileParser;
import net.vdanker.parser.model.GitTreeObject;
import net.vdanker.parser.model.JavaStats;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class InputStreamMapper {
    public static JavaStats toJavaStats(InputStream is) {
        try {
            return JavaFileParser.parse(null, is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static JavaStats toJavaStats(GitTreeObject to) {
        try {
            System.out.println(to.objectId() + " " + to.name());
            return JavaFileParser.parse(to.objectId(), to.is());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static XMLParser.DocumentContext parseXml(GitTreeObject to) {
        try {
            return XmlFileParser.parse(to.is());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static JavaParser toParser(InputStream is) {
        try {
            CharStream charStream = CharStreams.fromStream(is);
            var lexer = new parsers.JavaLexer(charStream);
            var tokens = new CommonTokenStream(lexer);
            return new parsers.JavaParser(tokens);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<Token> toTokenEmitter(InputStream is) {
        try {
            CharStream charStream = CharStreams.fromStream(is);
            var lexer = new parsers.JavaLexer(charStream);
            var tokens = new CommonTokenStream(lexer);
            tokens.fill();

            return tokens.getTokens();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
