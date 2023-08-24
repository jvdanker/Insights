package net.vdanker;

public record Churn(String project, String className, String fullPath, int statements, int complexity, int churn,
                    java.sql.Date epoch) {
}
