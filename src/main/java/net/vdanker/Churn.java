package net.vdanker;

public record Churn(String project, String className, String fullPath, int statements, int complexity,
                    String commitId,
                    java.sql.Date epoch) {
}
