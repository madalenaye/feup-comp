package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ast.PreorderJmmVisitor;
import pt.up.fe.comp2024.ast.TypeUtils;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends PreorderJmmVisitor<Void, OllirExprResult> {

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private static final String NEW = "new";
    private static final String INVOKESPECIAL = "invokespecial";
    private static final String INVOKESTATIC = "invokestatic";



    private final String END_STMT = ";\n";

    private final SymbolTable table;
    private String currMethod;
    public void setCurrMethod(String methodName) {
        this.currMethod = methodName;
    }

    public OllirExprGeneratorVisitor(SymbolTable table) {
        this.table = table;
    }

    @Override
    protected void buildVisitor() {
        addVisit(VAR_REF_EXPR, this::visitVarRef);
        addVisit(BINARY_EXPR, this::visitBinExpr);
        addVisit(INTEGER_LITERAL, this::visitInteger);
        addVisit(NEW_OBJECT_EXPR, this::visitNewObjExpr);
        addVisit(METHOD_EXPR,this::visitMethodExpr);


        setDefaultVisit(this::defaultVisit);
    }



    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        var intType = new Type(TypeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {

        var lhs = visit(node.getJmmChild(0));
        var rhs = visit(node.getJmmChild(1));

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // code to compute self
        Type resType = TypeUtils.getExprType(node, table,currMethod);
        String resOllirType = OptUtils.toOllirType(resType);
        String code = OptUtils.getTemp() + resOllirType;

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhs.getCode()).append(SPACE);

        Type type = TypeUtils.getExprType(node, table,currMethod);
        computation.append(node.get("op")).append(OptUtils.toOllirType(type)).append(SPACE)
                .append(rhs.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
    }


    private OllirExprResult visitVarRef(JmmNode node, Void unused) {

        var id = node.get("name");
        Type type = TypeUtils.getExprType(node, table,currMethod);
        String ollirType = OptUtils.toOllirType(type);

        String code = id + ollirType;

        return new OllirExprResult(code);
    }

    private OllirExprResult visitNewObjExpr(JmmNode node, Void unused){
        StringBuilder computation = new StringBuilder();

        String resType = node.get("name");
        String resOllirType = "."+resType;
        String code = OptUtils.getTemp() + resOllirType;

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(NEW+"(").append(resType).append(")").append(resOllirType)
                .append(END_STMT);

        computation.append(INVOKESPECIAL+"(")
                .append(code)
                .append(", \"\").V")
                .append(END_STMT);

        return new OllirExprResult(code, computation);
    }

    private OllirExprResult visitMethodExpr(JmmNode node, Void unused){
        StringBuilder code = new StringBuilder();
        StringBuilder computation = new StringBuilder();

        Type type = TypeUtils.getExprType(node.getJmmChild(1), table,currMethod);
        String ollirType = OptUtils.toOllirType(type);

        code.append(INVOKESTATIC)
                .append("(").append(node.getJmmChild(0).get("name"))
                .append(", \"").append(node.get("method"))
                .append("\", ").append(node.getJmmChild(1).get("name")).append(ollirType)
                .append(").V").append(END_STMT);

        return new OllirExprResult(code.toString(), computation.toString());
    }
    /**
     * Default visitor. Visits every child node and return an empty result.
     *
     * @param node
     * @param unused
     * @return
     */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {

        for (var child : node.getChildren()) {
            visit(child);
        }

        return OllirExprResult.EMPTY;
    }

}
