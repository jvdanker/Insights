package net.vdanker.parser;

import parsers.XMLLexer;
import parsers.XMLParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.IOException;
import java.io.InputStream;

public class XmlFileParser {
    public static XMLParser.DocumentContext parse(InputStream is) throws IOException {
        var charStream = CharStreams.fromStream(is);

        var lexer = new XMLLexer(charStream);
        var tokens = new CommonTokenStream(lexer);
        var parser = new XMLParser(tokens);

        return parser.document();
    }
}