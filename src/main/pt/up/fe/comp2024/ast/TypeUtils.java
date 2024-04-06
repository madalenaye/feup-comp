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
    public static Type getExprType(JmmNode expr, SymbolTable table) {

        var kind = Kind.fromString(expr.getKind());

        Type type = switch (kind) {
            case BINARY_EXPR -> getBinExprType(expr);
            case VAR_REF_EXPR -> getVarExprType(expr, table);
            case INTEGER_LITERAL -> new Type(INT_TYPE_NAME, false);
            case BOOLEAN_LITERAL -> new Type(BOOL_TYPE_NAME, false);
            case NEW_ARRAY_EXPR -> new Type(INT_TYPE_NAME, true);
            default -> throw new UnsupportedOperationException("Can't compute type for expression kind '" + kind + "'");
        };

        return type;
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


    private static Type getVarExprType(JmmNode varRefExpr, SymbolTable table) {
        for  (var field : table.getFields()) {
            if (field.getName().equals(varRefExpr.get("name")))
                return field.getType();
        }
        var method = varRefExpr.getAncestor(Kind.METHOD_DECL);
        if (method.isPresent()) {
            JmmNode m = method.get();
            for (var local : table.getLocalVariables(m.get("name"))) {
                if (local.getName().equals(varRefExpr.get("name")))
                    return local.getType();
            }
        }

        return new Type(INT_TYPE_NAME, false);
    }


    /**
     * @param sourceType
     * @param destinationType
     * @return true if sourceType can be assigned to destinationType
     */
    public static boolean areTypesAssignable(Type sourceType, Type destinationType) {

        return sourceType.equals(destinationType);
    }
}
