package net.vdanker.parser;

import java_parser.JavaLexer;
import java_parser.JavaParser;
import net.vdanker.parser.model.JavaMethod;
import net.vdanker.parser.model.JavaStats;
import org.antlr.v4.gui.TreeViewer;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeListener;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import javax.swing.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class JavaFileParser {
    public static Pair<String, JavaStats> parse(InputStream is) throws IOException {
        var charStream = CharStreams.fromStream(is);

        var lexer = new JavaLexer(charStream);
        var tokens = new CommonTokenStream(lexer);
        var parser = new JavaParser(tokens);

        var tree = parser.compilationUnit();
        var listener = new JavaListener(parser);

        var walker = new ParseTreeWalker();
        walker.walk((ParseTreeListener) listener, tree);

        JFrame frame = new JFrame("Antlr AST");
        JPanel container = new JPanel();
        JScrollPane scrollPane = new JScrollPane(container, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        frame.setContentPane(scrollPane);

        TreeViewer viewer = new TreeViewer(Arrays.asList(parser.getRuleNames()),tree);
        viewer.setScale(1.5);
        container.add(viewer);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

        return new Pair(
                listener.getFQClassName(),
                listener.getStats());
    }
}