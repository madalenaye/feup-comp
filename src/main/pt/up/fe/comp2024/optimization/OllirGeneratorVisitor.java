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

    private static final String SPACE = " ";
    private static final String ASSIGN = ":=";
    private final String END_STMT = ";\n";
    private final String NL = "\n";
    private final String L_BRACKET = " {\n";
    private final String R_BRACKET = "}\n";


    private final SymbolTable table;
    private final OllirStmtGeneratorVisitor stmtVisitor;
    private String currMethod;


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
        code.append(".field ");

        boolean isPublic = NodeUtils.getBooleanAttribute(node, "isPublic", "false");

        if (isPublic) {
            code.append("public ");
        }

        boolean isStatic = NodeUtils.getBooleanAttribute(node, "isStatic", "false");

        if (isStatic) {
            code.append("static ");
        }


        var typeCode = OptUtils.toOllirType(node.getJmmChild(0));
        var id = node.get("name");

        code.append(id);
        code.append(typeCode);

        code.append(END_STMT);

        return code.toString();
    }


    private String visitMethodDecl(JmmNode node, Void unused) {

        currMethod = node.get("name");
        stmtVisitor.setCurrMethod(currMethod);
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

        if(!params.isEmpty()){
            String paramCode = visit(params.get(0));
            code.append(paramCode);

            for (int i=1; i<params.size();i++) {
                code.append(", ");
                paramCode = visit(params.get(i));
                code.append(paramCode);
            }
        }



        code.append(")");

        // type
        String retType = OptUtils.toOllirType(node.getJmmChild(0));
        code.append(retType);
        code.append(L_BRACKET);


        List<JmmNode> stmts = NodeUtils.getStmts(node);
        if (stmts.isEmpty()) {
            code.append("ret.V");
            code.append(END_STMT);
        } else {
            // Otherwise, append statements
            for (JmmNode stmt : stmts) {
                String stmtCode = stmtVisitor.visit(stmt);
                code.append(stmtCode);
            }
        }


        code.append(R_BRACKET);
        code.append(NL);

        return code.toString();
    }


    private String visitClass(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        code.append(table.getClassName());

        code.append(" extends ");
        if(!table.getSuper().isEmpty()){
            code.append(table.getSuper());
        }else {
            code.append("Object");
        }
        code.append(L_BRACKET);

        code.append(NL);
        var needNl = true;

        for (var child : node.getChildren()) {
            var result = visit(child);

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

        String importNames = node.get("name");
        List<String> importList = Arrays.stream(importNames.substring(1, importNames.length() - 1).split(","))
                .map(String::trim)
                .toList();

        code.append("import ");
        for (int i = 0; i < importList.size(); i++) {
            code.append(importList.get(i));
            if (i < importList.size() - 1) {
                code.append(".");
            }
        }

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

        for (var child : node.getChildren()) {
            visit(child);
        }

        return "";
    }
}
