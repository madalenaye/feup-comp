package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static pt.up.fe.comp2024.ast.Kind.*;

/**
 * Generates OLLIR code from JmmNodes that are not expressions.
 */
public class OllirGeneratorVisitor extends AJmmVisitor<Void, String> {

    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";

    private final SymbolTable table;
    private final OllirStmtGeneratorVisitor stmtVisitor;


    public OllirGeneratorVisitor(SymbolTable table) {
        this.table = table;
        stmtVisitor = new OllirStmtGeneratorVisitor(table);
    }


    @Override
    protected void buildVisitor() {

        addVisit(PROGRAM, this::visitProgram);
        addVisit(IMPORT_DECL, this::visitImport);
        addVisit(CLASS_DECL, this::visitClass);
        addVisit(VAR_DECL,this::visitVarDecl);
        addVisit(METHOD_DECL, this::visitMethodDecl);
        addVisit(PARAM, this::visitParam);

        setDefaultVisit(this::defaultVisit);
    }



    private String visitParam(JmmNode node, Void unused) {

        var typeCode = OptUtils.toOllirType(node.getJmmChild(0));
        var id = node.get("name");

        String code = id + typeCode;

        return code;
    }

    private String visitVarDecl(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        code.append(".field public ");

        var typeCode = OptUtils.toOllirType(node.getJmmChild(0));
        var id = node.get("name");

        code.append(id);
        code.append(typeCode);

        code.append(END_STMT);

        return code.toString();
    }


    private String visitMethodDecl(JmmNode node, Void unused) {

        stmtVisitor.setCurrMethod(node.get("name"));

        StringBuilder code = new StringBuilder(".method ");

        boolean isPublic = NodeUtils.getBooleanAttribute(node, "isPublic", "false");

        if (isPublic) {
            code.append("public ");
        }

        boolean isStatic = NodeUtils.getBooleanAttribute(node, "isStatic", "false");

        if (isStatic) {
            code.append("static ");
        }

        // name
        String name = node.get("name");
        code.append(name);

        // params
        List<JmmNode> params = node.getChildren(PARAM);
        code.append("(");

        List<String> parameters = params.stream().map(this::visit).toList();
        String paramsCode = String.join(", ", parameters);
        code.append(paramsCode);

        code.append(")");

        // type
        String retType = OptUtils.toOllirType(node.getJmmChild(0));
        code.append(retType);
        code.append(L_BRACKET);

        List<JmmNode> stmts = NodeUtils.getStmts(node);
        for (JmmNode stmt : stmts) {
            String stmtCode = stmtVisitor.visit(stmt);
            code.append(stmtCode);
        }
        if (name.equals("main")) {
            code.append("ret.V");
            code.append(END_STMT);
        }

        code.append(R_BRACKET);
        code.append(NL);

        return code.toString();
    }


    private String visitClass(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        code.append(table.getClassName());


        if (!table.getSuper().isEmpty()) {
            code.append(" extends ").append(table.getSuper());
        } else code.append(" extends ").append("Object");
        code.append(L_BRACKET);

        code.append(NL);
        boolean needNl = true;

        for (JmmNode child : node.getChildren()) {
            String result = visit(child);

            if (METHOD_DECL.check(child) && needNl) {
                code.append(NL);
                needNl = false;
            }

            code.append(result);
        }

        code.append(buildConstructor());
        code.append(R_BRACKET);

        return code.toString();
    }

    private String buildConstructor() {

        return ".construct " + table.getClassName() + "().V {\n" +
                "invokespecial(this, \"<init>\").V;\n" +
                "}\n";
    }

    private String visitImport(JmmNode node, Void unused) {
        StringBuilder code = new StringBuilder();
        code.append("import ");

        List<String> importPath = node.getObjectAsList("name", String.class);
        String path = String.join(".", importPath);
        code.append(path);
        code.append(END_STMT);
        return code.toString();
    }

    private String visitProgram(JmmNode node, Void unused) {


        StringBuilder code = new StringBuilder();

        node.getChildren().stream()
                .map(this::visit)
                .forEach(code::append);



        return code.toString();
    }

    /**
     * Default visitor. Visits every child node and return an empty string.
     *
     * @param node
     * @param unused
     * @return
     */
    private String defaultVisit(JmmNode node, Void unused) {

        for (JmmNode child : node.getChildren()) {
            visit(child);
        }

        return "";
    }
}
