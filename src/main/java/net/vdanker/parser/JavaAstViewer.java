package net.vdanker.parser;

import java_parser.JavaParser;
import org.antlr.v4.gui.TreeViewer;

import javax.swing.*;
import java.io.IOException;
import java.util.Arrays;

public class JavaAstViewer {
    public static void showAst(Pair<JavaParser, JavaParser.CompilationUnitContext> pair) {
        JFrame frame = new JFrame("Antlr AST");
        JPanel container = new JPanel();
        JScrollPane scrollPane = new JScrollPane(container, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        frame.setContentPane(scrollPane);

        TreeViewer viewer = new TreeViewer(Arrays.asList(pair.key.getRuleNames()), pair.value);
        viewer.setScale(1.0);
        container.add(viewer);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}