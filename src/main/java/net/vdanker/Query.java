package net.vdanker;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.*;
import java.sql.Date;
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
                String.format("%s.%s.%s,%d,%d,%s,%s,%d,%s",
                        e.packageName,
                        e.className,
                        e.methodName,
                        e.statements,
                        e.complexity,
                        e.project,
                        e.fullPath,
                        e.lineStart,
                        e.epoch
                )
        );

        return result;
    }

    public static void main(String[] args) {
        generateAndSave();
    }

    public static void generateAndSave() {
        try {
            generateData();
            generateProjects();
            generateChurn();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private static void generateChurn() {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "sa")) {
            try (PreparedStatement s = connection.prepareStatement(
                    """
                        select m.PROJECT
                             , m.CLASS
                             , m.FULLPATH
                             , m.STATEMENTS
                             , m.COMPLEXITY
                             , count(de.id) as churn
                             , max(c.epoch) as epoch
                        from commits c
                                 inner join DIFFENTRIES de on c.COMMIT_ID = de.COMMIT1
                                 inner join (select f.PROJECT
                                                  , m.CLASS
                                                  , f.FULLPATH
                                                  , sum(m.STATEMENTS) as statements
                                                  , sum(m.COMPLEXITY) as complexity
                                             from files f
                                                      inner join methods m on f.OBJECT_ID = m.FILE_ID
                                             group by f.PROJECT, m.class, f.FULLPATH) m
                                 on de.FULLPATH = m.FULLPATH
                        where c.EPOCH > DATEADD('MONTH', -6, CURRENT_DATE)
                        group by m.PROJECT, m.CLASS, m.FULLPATH, m.STATEMENTS, m.COMPLEXITY
                        """)) {
                ResultSet resultSet = s.executeQuery();

                List<Churn> entries = new ArrayList<>();
                while (resultSet.next()) {
                    String project = resultSet.getString(1);
                    String className = resultSet.getString(2);
                    String fullPath = resultSet.getString(3);
                    int statements = resultSet.getInt(4);
                    int complexity = resultSet.getInt(5);
                    int churn = resultSet.getInt(6);
                    Date epoch = resultSet.getDate(7);

                    entries.add(
                            new Churn(
                                    project,
                                    className,
                                    fullPath,
                                    statements,
                                    complexity,
                                    churn,
                                    epoch
                            )
                    );
                }

                String fileName = "treemap-stratify/files/churn.csv";
                BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
                writer.write("project,className,fullPath,statements,complexity,churn,epoch\n");

                entries
                        .forEach(p -> {
                            try {
                                writer.write(String.format("%s,%s,%s,%d,%d,%d,%s\n",
                                        p.project(),
                                        p.className(),
                                        p.fullPath(),
                                        p.statements(),
                                        p.complexity(),
                                        p.churn(),
                                        p.epoch()
                                        ));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });

                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    static void generateData() throws ClassNotFoundException, SQLException {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "sa")) {
            try (PreparedStatement s = connection.prepareStatement(
                    """
                        select package
                            , class
                            , method
                            , statements
                            , complexity
                            , f.PROJECT
                            , f.FULLPATH
                            , m.LINESTART
                            , c.EPOCH
                          from methods m
                        inner join files f on m.FILE_ID = f.OBJECT_ID
                        inner join DIFFENTRIES de on f.OBJECT_ID = de.NEWID
                        inner join COMMITS c on de.COMMIT1 = c.COMMIT_ID
                        """)) {
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
                    Date epoch = resultSet.getDate(9);

                    entries.add(
                            new Entry(
                                    project,
                                    packageName,
                                    className,
                                    methodName,
                                    statements,
                                    complexity,
                                    fullPath,
                                    lineStart,
                                    epoch
                            )
                    );
                }

                Tree tree = new Tree("root", -1, new HashMap<>());
                entries.forEach(e -> parseEntry(e, tree));

//                System.out.println(tree);

                String fileName = "treemap-stratify/files/data.csv";
                BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
                writer.write("name,size,complexity,project,fullPath,lineStart,epoch\n");

                entries.stream()
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

    static void generateProjects() throws ClassNotFoundException, SQLException {
        try (Connection connection = DriverManager.getConnection(URL, "sa", "sa")) {
            try (PreparedStatement s = connection.prepareStatement(
                    "select distinct project from files order by project")) {
                ResultSet resultSet = s.executeQuery();

                String fileName = "treemap-stratify/files/projects.csv";
                BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
                writer.write("name\n");

                while (resultSet.next()) {
                    String project = resultSet.getString(1);
                    writer.write(String.format("%s,\n", project));
                }

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
            int lineStart, Date epoch) {

    }

}
