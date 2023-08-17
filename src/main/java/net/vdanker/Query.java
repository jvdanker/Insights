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
        StringTokenizer st = new StringTokenizer(
                String.format("%s.%s", e.packageName, e.className),
                ".");

        while (st.hasMoreTokens()) {
            if (!buf.isEmpty()) {
                buf.append(".");
            }

            String t = st.nextToken();
            buf.append(t);
            result.add(buf.toString());
        }

        result.add(
                String.format("%s.%s.%s,%d,%d,%s,%s,%d",
                        e.packageName,
                        e.className,
                        e.methodName,
                        e.statements,
                        e.complexity,
                        e.project,
                        e.fullPath,
                        e.lineStart)
        );

        return result;
    }

    public static void main(String[] args) {
        generateAndSave();
    }

    public static void generateAndSave() {
        try {
            generate();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    static void generate() throws ClassNotFoundException, SQLException {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "sa")) {
            try (PreparedStatement s = connection.prepareStatement(
                    "select package" +
                            "    , class" +
                            "    , method" +
                            "    , statements" +
                            "    , complexity" +
                            "    , f.PROJECT" +
                            "    , f.FULLPATH" +
                            "    , m.LINESTART" +
                            "  from methods m " +
                            "inner join files f on m.FILE_ID = f.OBJECT_ID " +
                            "where not method in ('equals', 'hashCode')")) {
                ResultSet resultSet = s.executeQuery();

                List<Entry> entries = new ArrayList<>();
                while (resultSet.next()) {
                    String packageName = resultSet.getString(1);
                    String className = resultSet.getString(2);
                    String methodName = resultSet.getString(3);
                    int statements = resultSet.getInt(4);
                    int complexity = resultSet.getInt(5);
                    String project = resultSet.getString(6);
                    String fullPath = resultSet.getString(7);
                    int lineStart = resultSet.getInt(8);

                    entries.add(
                            new Entry(
                                    project,
                                    packageName,
                                    className,
                                    methodName,
                                    statements,
                                    complexity,
                                    fullPath,
                                    lineStart
                            )
                    );
                }

                Tree tree = new Tree("root", -1, new HashMap<>());
                entries.forEach(e -> parseEntry(e, tree));

//                System.out.println(tree);

                String fileName = "treemap-stratify/files/data.csv";
                BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
                writer.write("name,size,complexity,project,fullPath,lineStart\n");

                entries.stream()
//                        .filter(e -> e.statements == 1)
//                        .filter(e -> !e.methodName.startsWith("is"))
//                        .filter(e -> !e.methodName.startsWith("set"))
//                        .filter(e -> !e.methodName.startsWith("get"))
//                        .filter(e -> e.statements > 50)
//                        .filter(e -> e.complexity > 20)
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

                writer.close();
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
            String project,
            String packageName,
            String className,
            String methodName,
            int statements,
            int complexity,
            String fullPath,
            int lineStart) {

    }

}
