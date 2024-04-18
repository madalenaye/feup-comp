package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.Objects;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are statements.
 */
public class OllirStmtGeneratorVisitor extends AJmmVisitor<Void, String> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";

    private final SymbolTable table;
    private final OllirExprGeneratorVisitor exprVisitor;
    private String currMethod;

    public OllirStmtGeneratorVisitor(SymbolTable table) {
        this.table = table;
        exprVisitor = new OllirExprGeneratorVisitor(table);
    }

    public void setCurrMethod(String methodName) {
        this.currMethod = methodName;
        exprVisitor.setCurrMethod(currMethod);
    }

    @Override
    protected void buildVisitor() {
        addVisit(RETURN_STMT, this::visitReturn);
        addVisit(ASSIGN_STMT, this::visitAssignStmt);
        addVisit(EXPR_STMT, this::visitExprStmt);
        // addVisit(STMTS, this::visitStmts);
        setDefaultVisit(this::defaultVisit);
    }

    private String visitAssignStmt(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        // variable
        String variable = node.get("name");

        Type varType = TypeUtils.getVariableType(variable, table, currMethod);
        assert varType != null;
        String varOllirType = OptUtils.toOllirType(varType);

        JmmNode exprNode = node.getJmmChild(0);

        var expr = exprVisitor.visit(exprNode);

        String exprCode = expr.getCode();

        if (exprNode.getKind().equals("MethodExpr")) {
            Type type = TypeUtils.getExprType(exprNode, table, currMethod);
            String ollirType = OptUtils.toOllirType(Objects.requireNonNull(type));
            String newTmp = OptUtils.getTemp() + ollirType;
            code.append(newTmp)
                    .append(SPACE).append(ASSIGN).append(ollirType).append(SPACE)
                    .append(exprCode);
            exprCode = newTmp;
        }

        code.append(expr.getComputation());

        if (table.getFields().stream().anyMatch(field -> field.getName().equals(variable))) {
            code.append("putfield(this, ")
                    .append(variable)
                    .append(varOllirType)
                    .append(", ")
                    .append(exprCode)
                    .append(").V");
        } else{
            code.append(variable).append(varOllirType);
            code.append(SPACE + ASSIGN).append(varOllirType).append(SPACE);
            code.append(exprCode);
        }

        code.append(END_STMT);

        return code.toString();
    }


    private String visitReturn(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        Type retType = table.getReturnType(currMethod);

        OllirExprResult expr = exprVisitor.visit(node.getJmmChild(0));

        code.append(expr.getComputation());
        code.append("ret");
        code.append(OptUtils.toOllirType(retType));
        code.append(SPACE);

        code.append(expr.getCode());

        code.append(END_STMT);

        return code.toString();
    }

    private String visitExprStmt(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();

        var expr = exprVisitor.visit(node.getJmmChild(0));

        code.append(expr.getComputation());
        code.append(expr.getCode());

        return code.toString();
    }

    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private String defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return "";
    }

}
