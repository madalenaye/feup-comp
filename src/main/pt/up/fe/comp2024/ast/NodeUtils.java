package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static pt.up.fe.comp2024.ast.Kind.STATEMENTS;
import static pt.up.fe.comp2024.ast.Kind.STMTS;

public class NodeUtils {

    public static int getLine(JmmNode node) {

        return getIntegerAttribute(node, "lineStart", "-1");
    }

    public static int getColumn(JmmNode node) {

        return getIntegerAttribute(node, "colStart", "-1");
    }

    public static int getIntegerAttribute(JmmNode node, String attribute, String defaultVal) {
        String line = node.getOptional(attribute).orElse(defaultVal);
        return Integer.parseInt(line);
    }

    public static boolean getBooleanAttribute(JmmNode node, String attribute, String defaultVal) {
        String line = node.getOptional(attribute).orElse(defaultVal);
        return Boolean.parseBoolean(line);
    }

    public static List<JmmNode> getStmts(JmmNode node) {
        List<JmmNode> nodes = new ArrayList<>();
        for (JmmNode child : node.getChildren()) {
            if (child.isInstance("Stmt")) {
                nodes.add(child);
            }
        }
        return nodes;
    }


}
