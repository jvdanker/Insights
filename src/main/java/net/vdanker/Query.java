package net.vdanker;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class Query {

    static String URL = "jdbc:h2:tcp://localhost:9092/./test";

    static List<String> permutationsOf(Entry e) {
        List<String> result = new ArrayList<>();

        StringBuilder buf = new StringBuilder();
        StringTokenizer st = new StringTokenizer(String.format("%s.%s", e.packageName, e.className), ".");
        while (st.hasMoreTokens()) {
            if (!buf.isEmpty()) {
                buf.append(".");
            }

            String t = st.nextToken();
            buf.append(t);
            result.add(buf.toString());
        }

        result.add(String.format("%s.%s.%s,%d", e.packageName, e.className, e.methodName, e.statements));
        return result;
    }

    public static void main(String[] args) throws ClassNotFoundException, SQLException {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "sa")) {
            try (PreparedStatement s = connection.prepareStatement("SELECT * FROM methods")) {
                ResultSet resultSet = s.executeQuery();

                List<Entry> entries = new ArrayList<>();
                while (resultSet.next()) {
                    String packageName = resultSet.getString(3);
                    String className = resultSet.getString(4);
                    String methodName = resultSet.getString(5);
                    int statements = resultSet.getInt(6);
                    entries.add(new Entry(packageName, className, methodName, statements));
                }

                Tree tree = new Tree("root", -1, new HashMap<>());
                entries.forEach(e -> parseEntry(e, tree));

//                System.out.println(tree);

                String fileName = "/Users/juan/Downloads/treemap-stratify/files/data.csv";
                BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
                writer.write("name,size\n");

                entries.stream()
                        .filter(e -> e.statements > 1)
                        .map(Query::permutationsOf)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toSet())
                        .stream().sorted(Comparator.comparingInt(String::length))
                        .forEach(p -> {
                            try {
                                writer.write(String.format("%s,\n", p));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });

//                entries.forEach(e -> {
//                    try {
//                        int size = e.statements;
//                        writer.write(String.format("%s.%s.%s,%d\n", e.packageName, e.className, e.methodName, size));
//                    } catch (IOException ex) {
//                        throw new RuntimeException(ex);
//                    }
//                });

                writer.close();

//                String fileName = "/Users/juan/Downloads/pack_2/files/data.json";
//                BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
//                printTree(writer, tree);
//                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static void parseEntry(Entry entry, Tree tree) {
        String packageName = entry.packageName();
        StringTokenizer stringTokenizer = new StringTokenizer(packageName, ".");

        Tree current = tree;
        while (stringTokenizer.hasMoreTokens()) {
            String s1 = stringTokenizer.nextToken();
            if (current.tree().containsKey(s1)) {
                current = current.tree().get(s1);
            } else {
                Tree leaf = new Tree(s1, -1, new HashMap<>());
                current.tree().put(s1, leaf);
                current = leaf;
            }
        }

        Tree classNameChild = new Tree(entry.className(), -1, new HashMap<>());
        current.tree().put(entry.className(), classNameChild);

        Tree methodNameChild = new Tree(entry.methodName(), entry.statements, null);
        classNameChild.tree().put(entry.methodName(), methodNameChild);
    }

    private static void printTree(BufferedWriter writer, Tree tree) {
        tree.tree().forEach((k,v) -> {
            try {
                printLeave(writer, 0, v);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void printLeave(BufferedWriter writer, int index, Tree tree) throws IOException {
        String tabs = "\t".repeat(index);
        String tabs1 = "\t".repeat(index + 1);

        writer.write(tabs + "{");
        writer.write(tabs1 + "\"name\": \"" + tree.name() + "\",");
        writer.write(tabs1 + "\"children\": [");
        tree.tree.forEach((k,v) -> {
            try {
                printChild(writer, index + 1, v);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        writer.write(tabs1 + "]");
        writer.write(tabs + "}, ");
    }

    private static void printChild(BufferedWriter writer, int index, Tree tree) throws IOException {
        String tabs = "\t".repeat(index);
        String tabs1 = "\t".repeat(index + 1);

        if (tree.tree == null) {
            writer.write(tabs1 + "{\"name\": \"" + tree.name + "\", \"value\": " + tree.value + "}");
        } else {
            printLeave(writer, index + 1, tree);
        }
    }

    record Children(Map<String, Child> children) {}

    record Child(String name, Children children, int value) {}

    record Tree(String name, int value, Map<String, Tree> tree) {}


    record Entry(
            String packageName,
            String className,
            String methodName,
            int statements) {}

}
