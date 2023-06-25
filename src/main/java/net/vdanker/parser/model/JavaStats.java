package net.vdanker.parser.model;

import java.util.List;

public record JavaStats(List<String> imports, List<JavaMethod> methods) {
}
