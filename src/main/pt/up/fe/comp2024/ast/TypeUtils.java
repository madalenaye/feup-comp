package pt.up.fe.comp2024.ast;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;


public class TypeUtils {

    private static final String INT_TYPE_NAME = "int";
    private static final String BOOL_TYPE_NAME = "boolean";


    public static String getIntTypeName() {
        return INT_TYPE_NAME;
    }
    public static String getBoolTypeName() { return BOOL_TYPE_NAME; }


    public static boolean isArray(JmmNode type) {
        return Boolean.parseBoolean(type.get("isArray"));
    }
    /**
     * Gets the {@link Type} of an arbitrary expression.
     *
     * @param expr
     * @param table
     * @return
     */
    public static Type getExprType(JmmNode expr, SymbolTable table, String method) {

        if (expr.hasAttribute("notDeclared")) {
            return null;
        }

        if (expr.hasAttribute("type")) {
            return expr.getObject("type", Type.class);
        }

        var kind = Kind.fromString(expr.getKind());

        Type type = switch (kind) {
            case INTEGER_LITERAL, ARRAY_ELEM_EXPR, LEN_EXPR -> new Type(INT_TYPE_NAME, false);
            case BOOLEAN_LITERAL, NEG_EXPR -> new Type(BOOL_TYPE_NAME, false);
            case NEW_ARRAY_EXPR, ARRAY_EXPR -> new Type(INT_TYPE_NAME, true);
            case PARENS_EXPR -> getExprType(expr.getChild(0), table, method);
            case THIS_EXPR -> new Type(table.getClassName(), false);
            case BINARY_EXPR -> getBinExprType(expr);
            case VAR_REF_EXPR -> getVarExprType(expr, table, method);
            case NEW_OBJECT_EXPR -> new Type(expr.get("name"), false);
            case METHOD_EXPR -> getMethodExprType(expr, table, method);
            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };

        if (type != null) {
            expr.putObject("type", type);
        } else {
            expr.putObject("notDeclared", true);
        }

        return type;
    }

    public static Type getMethodExprType(JmmNode expr, SymbolTable table, String currentMethod) {
        JmmNode node = expr.getChild(0);
        String method = expr.get("method");

        if (node.getKind().equals("ThisExpr")) {
            return table.getReturnType(method);
        }

        if (node.getKind().equals("VarRefExpr")) {
            Type varType = getVarExprType(node, table, currentMethod);
            if (varType == null) {
                return null;
            } else if (varType.hasAttribute("isExternal")) {
                return varType;
            } else if (table.getImports().contains(varType.getName())) {
                varType.putObject("isExternalObject", true);
                return varType;
            }
            String className = varType.getName();
            if (className.equals(table.getClassName()))
                return table.getReturnType(method);
        }

        if (!node.getKind().equals("MethodExpr")) {
            return null;
        }

        Type nodeType = getMethodExprType(node, table, currentMethod);
        if (nodeType == null) {
            return null;
        }

        if (nodeType.hasAttribute("isExternal") || nodeType.hasAttribute("isExternalObject")) {
            return nodeType;
        }

        String className = nodeType.getName();
        if (className.equals(table.getClassName()))
            return table.getReturnType(method);

        return null;
    }

    private static Type getBinExprType(JmmNode binaryExpr) {

        String operator = binaryExpr.get("op");

        return switch (operator) {
            case "+", "*", "-", "/" -> new Type(INT_TYPE_NAME, false);
            case "&&", "<" -> new Type(BOOL_TYPE_NAME, false);
            default ->
                    throw new RuntimeException("Unknown operator '" + operator + "' of expression '" + binaryExpr + "'");
        };
    }

    public static Type getVarExprType(JmmNode varRefExpr, SymbolTable table, String method) {

        String variable = varRefExpr.get("name");

        // Var is a field
        for (var field : table.getFields()) {
            if (field.getName().equals(variable)) {
                return field.getType();
            }
        }

        // Var is a local
        for (var local : table.getLocalVariables(method)) {
            if (local.getName().equals(variable)) {
                return local.getType();
            }
        }

        // Var is a parameter
        for (var param : table.getParameters(method)) {
            if (param.getName().equals(variable)) {
                return param.getType();
            }
        }

        // Var is imported
        if (table.getImports().contains(variable)) {
            Type type = new Type(variable, false);
            type.putObject("isExternal", true);
            return type;
        }
        return null;
    }

    public static boolean isIntType(Type type) {
        return type.getName().equals(INT_TYPE_NAME) && !type.isArray();
    }

    public static boolean isBoolType(Type type) {
        return type.getName().equals(BOOL_TYPE_NAME) && !type.isArray();
    }

    public static boolean isObjectType(Type type) {
        return !type.isArray() && !type.getName().equals(INT_TYPE_NAME) && !type.getName().equals(BOOL_TYPE_NAME);
    }

    public static Type getVariableType(String variable, SymbolTable table, String method) {
        // Var is a field
        for (var field : table.getFields()) {
            if (field.getName().equals(variable)) {
                return field.getType();
            }
        }

        // Var is a local
        for (var local : table.getLocalVariables(method)) {
            if (local.getName().equals(variable)) {
                return local.getType();
            }
        }

        // Var is a parameter
        for (var param : table.getParameters(method)) {
            if (param.getName().equals(variable)) {
                return param.getType();
            }
        }

        return null;
    }

}
