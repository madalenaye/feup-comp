package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.TypeUtils;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are expressions.
 */
public class OllirExprGeneratorVisitor extends AJmmVisitor<Void, OllirExprResult> {

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
        addVisit(NEG_EXPR, this::visitNegExpr);
        addVisit(BOOLEAN_LITERAL, this::visitBoolean);
        addVisit(PARENS_EXPR, this::visitParensExpr);
        setDefaultVisit(this::defaultVisit);
    }



    private OllirExprResult visitInteger(JmmNode node, Void unused) {
        Type intType = new Type(TypeUtils.getIntTypeName(), false);
        String ollirIntType = OptUtils.toOllirType(intType);
        String code = node.get("value") + ollirIntType;
        return new OllirExprResult(code);
    }

    private OllirExprResult visitBoolean(JmmNode node, Void unused) {
        Type boolType = new Type(TypeUtils.getBoolTypeName(), false);
        String ollirBoolType = OptUtils.toOllirType(boolType);
        String code = OptUtils.toOllirBool(node.get("value")) + ollirBoolType;
        return new OllirExprResult(code);
    }


    private OllirExprResult visitBinExpr(JmmNode node, Void unused) {

        OllirExprResult lhs = visit(node.getJmmChild(0));
        OllirExprResult rhs = visit(node.getJmmChild(1));

        StringBuilder computation = new StringBuilder();

        // code to compute the children
        computation.append(lhs.getComputation());
        computation.append(rhs.getComputation());

        // code to compute self
        Type resType = TypeUtils.getExprType(node, table, currMethod);
        assert resType != null;
        String resOllirType = OptUtils.toOllirType(resType);
        String code = OptUtils.getTemp() + resOllirType;

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType).append(SPACE)
                .append(lhs.getCode()).append(SPACE);

        Type type = TypeUtils.getExprType(node, table, currMethod);
        assert type != null;
        computation.append(node.get("op")).append(OptUtils.toOllirType(type)).append(SPACE)
                .append(rhs.getCode()).append(END_STMT);

        return new OllirExprResult(code, computation);
    }


    private OllirExprResult visitVarRef(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();
        String code;

        String id = node.get("name");
        Type type = TypeUtils.getExprType(node, table, currMethod);
        assert type != null;
        String ollirType = OptUtils.toOllirType(type);


        var fields = table.getFields();
        boolean variableIsField = fields.stream()
                .anyMatch(field -> field.getName().equals(id));

        if(variableIsField){
            code = OptUtils.getTemp() + ollirType;
            computation.append(code)
                    .append(SPACE).append(ASSIGN).append(ollirType).append(SPACE)
                    .append("getfield(this, ").append(ollirType).append(")")
                    .append(ollirType).append(END_STMT);
        }else{
            code = id + ollirType;
        }


        return new OllirExprResult(code,computation);
    }

    private OllirExprResult visitNewObjExpr(JmmNode node, Void unused){
        StringBuilder computation = new StringBuilder();

        String resType = node.get("name");
        String resOllirType = "." + resType;
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

        Type type = TypeUtils.getExprType(node.getJmmChild(0), table, currMethod);
        String ollirType = OptUtils.toOllirType(type);

        if (table.getImports().contains(type.getName())) {
            code.append(INVOKESTATIC)
                    .append("(").append(node.getJmmChild(0).get("name"))
                    .append(", \"").append(node.get("method"))
                    .append("\", ").append(node.getJmmChild(0).get("name")).append(ollirType)
                    .append(").V").append(END_STMT);
        }
        else {
            code.append("invokevirtual")
                    .append("(").append(node.getJmmChild(0).get("name"))
                    .append(", \"").append(node.get("method"))
                    .append("\", ").append(node.getJmmChild(0).get("name")).append(ollirType)
                    .append(").V").append(END_STMT);
        }



        return new OllirExprResult(code.toString(), computation.toString());
    }

    private OllirExprResult visitParensExpr(JmmNode node, Void unused) {
        return this.visit(node.getJmmChild(0));
    }

    private OllirExprResult visitNegExpr(JmmNode node, Void unused) {
        StringBuilder computation = new StringBuilder();


        var expr = this.visit(node.getJmmChild(0));

        computation.append(expr.getComputation());

        Type resType = TypeUtils.getExprType(node, table, currMethod);
        assert resType != null;
        String resOllirType = OptUtils.toOllirType(resType);
        String code = OptUtils.getTemp() + resOllirType;

        computation.append(code).append(SPACE)
                .append(ASSIGN).append(resOllirType)
                .append(" !.bool ")
                .append(expr.getCode()).append(END_STMT);


        return new OllirExprResult(code,computation);
    }
        /**
         * Default visitor. Visits every child node and return an empty result.
         *
         * @param node
         * @param unused
         * @return
         */
    private OllirExprResult defaultVisit(JmmNode node, Void unused) {

        for (JmmNode child : node.getChildren()) {
            visit(child);
        }

        return OllirExprResult.EMPTY;
    }

}
