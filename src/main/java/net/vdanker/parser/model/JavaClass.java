package net.vdanker.parser.model;

import java.util.ArrayList;
import java.util.List;

public record JavaClass(
        String fqName,
        List<JavaMethod> methods) {

}
