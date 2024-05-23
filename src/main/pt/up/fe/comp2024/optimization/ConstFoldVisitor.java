package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.JmmNodeImpl;
import pt.up.fe.comp2024.ast.Kind;

public class ConstFoldVisitor extends AJmmVisitor<Void, Boolean>  {

    @Override
    public void buildVisitor() {
        addVisit(Kind.BINARY_EXPR, this::visitBinaryExpr);
        addVisit(Kind.NEG_EXPR, this::visitNegExpr);
        addVisit(Kind.PARENS_EXPR, this::visitParensExpr);
        addVisit(Kind.WHILE_STMT, this::visitWhileStmt);
        addVisit(Kind.IF_STMT, this::visitIfStmt);
        setDefaultVisit(this::defaultVisit);
    }

    private Boolean visitBinaryExpr(JmmNode expr, Void unused) {

        boolean hasChanged = false;

        JmmNode left = expr.getChild(0);
        JmmNode right = expr.getChild(1);

        hasChanged |= visit(left);
        hasChanged |= visit(right);

        String operator = expr.get("op");

        hasChanged |= switch (operator) {
            case "+", "*", "-", "/" -> handleArithmetic(expr, operator);
            case "<" -> handleComparison(expr);
            case "&&" -> handleShortCircuit(expr);
            default -> false;
        };

        return hasChanged;
    }

    private Boolean handleArithmetic(JmmNode expr, String operator) {
        JmmNode left = expr.getChild(0);
        JmmNode right = expr.getChild(1);

        if (left.getKind().equals("IntegerLiteral") && right.getKind().equals("IntegerLiteral")) {
            int leftVal = Integer.parseInt(left.get("value"));
            int rightVal = Integer.parseInt(right.get("value"));

            // what if rightVal = 0 in a division?
            int val = switch (operator) {
                case "+" -> leftVal + rightVal;
                case "*" -> leftVal * rightVal;
                case "-" -> leftVal - rightVal;
                case "/" -> leftVal / rightVal;
                default -> throw new IllegalStateException("Unexpected value: " + operator);
            };

            JmmNodeImpl newExpr = new JmmNodeImpl("IntegerLiteral");
            newExpr.put("value", Integer.toString(val));

            expr.replace(newExpr);

            return true;
        }
        return false;
    }

    private Boolean handleComparison(JmmNode expr) {
        JmmNode left = expr.getChild(0);
        JmmNode right = expr.getChild(1);

        if (left.getKind().equals("IntegerLiteral") && right.getKind().equals("IntegerLiteral")) {
            int leftVal = Integer.parseInt(left.get("value"));
            int rightVal = Integer.parseInt(right.get("value"));

            boolean val = leftVal < rightVal;

            JmmNodeImpl newExpr = new JmmNodeImpl("BooleanLiteral");
            newExpr.put("value", Boolean.toString(val));

            expr.replace(newExpr);

            return true;
        }
        return false;
    }

    private Boolean handleShortCircuit(JmmNode expr) {
        JmmNode left = expr.getChild(0);
        JmmNode right = expr.getChild(1);

        if (left.getKind().equals("BooleanLiteral") && right.getKind().equals("BooleanLiteral")) {
            boolean leftVal = Boolean.parseBoolean(left.get("value"));
            boolean rightVal = Boolean.parseBoolean(right.get("value"));

            boolean val = leftVal && rightVal;

            JmmNodeImpl newExpr = new JmmNodeImpl("BooleanLiteral");
            newExpr.put("value", Boolean.toString(val));

            expr.replace(newExpr);

            return true;
        }
        return false;
    }

    private Boolean visitNegExpr(JmmNode expr, Void unused) {
        JmmNode child = expr.getChild(0);

        boolean hasChanged = visit(child);

        if (child.getKind().equals("BooleanLiteral")) {
            boolean val = !Boolean.parseBoolean(child.get("value"));

            JmmNodeImpl newExpr = new JmmNodeImpl("BooleanLiteral");
            newExpr.put("value", Boolean.toString(val));

            expr.replace(newExpr);

            return true;
        }
        return hasChanged;
    }

    private Boolean visitIfStmt(JmmNode ifStmt, Void unused) {
        JmmNode condition = ifStmt.getObject("condition", JmmNode.class);
        JmmNode thenStmt = ifStmt.getChild(1);
        JmmNode elseStmt = ifStmt.getChild(2);

        boolean hasChanged = visit(condition);

        if (condition.getKind().equals("BooleanLiteral")) {
            boolean val = Boolean.parseBoolean(condition.get("value"));
            if (val) {
                ifStmt.replace(thenStmt);
            } else {
                ifStmt.replace(elseStmt);
            }
            return true;
        }

        hasChanged |= visit(thenStmt);
        hasChanged |= visit(elseStmt);

        return hasChanged;
    }

    private Boolean visitParensExpr(JmmNode expr, Void unused) {
        JmmNode child = expr.getChild(0);

        boolean hasChanged = visit(child);

        if (child.getKind().equals("BooleanLiteral") || child.getKind().equals("IntegerLiteral")) {
            JmmNodeImpl newExpr = new JmmNodeImpl(child.getKind());
            newExpr.put("value", child.get("value"));

            expr.replace(newExpr);

            return true;
        }
        return hasChanged;
    }

    private Boolean visitWhileStmt(JmmNode whileStmt, Void unused) {
        JmmNode condition = whileStmt.getObject("condition", JmmNode.class);
        JmmNode body = whileStmt.getChild(1);

        boolean hasChanged = visit(condition);

        if (condition.getKind().equals("BooleanLiteral")) {
            boolean val = Boolean.parseBoolean(condition.get("value"));
            if (!val) {
                whileStmt.replace(body);
                return true;
            }
        }

        hasChanged |= visit(body);

        return hasChanged;
    }
    private Boolean defaultVisit(JmmNode node, Void unused) {
        boolean hasChanged = false;
        for(JmmNode child : node.getChildren()) {
            hasChanged |= visit(child);
        }
        return hasChanged;
    }
}
