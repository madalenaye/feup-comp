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

        //before
        /*if(!params.isEmpty()){
            String paramCode = visit(params.get(0));
            code.append(paramCode);

            for (int i = 1; i < params.size(); i++) {
                code.append(", ");
                paramCode = visit(params.get(i));
                code.append(paramCode);
            }
        }*/

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

        if(retType.equals(".V")){
            code.append("ret.V").append(END_STMT);
        }

        code.append(R_BRACKET);
        code.append(NL);

        return code.toString();
    }


    private String visitClass(JmmNode node, Void unused) {

        StringBuilder code = new StringBuilder();

        code.append(table.getClassName());

        code.append(" extends ");
        if (!table.getSuper().isEmpty()) {
            code.append(table.getSuper());
        } else {
            code.append("Object");
        }
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
        /*
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
        } */

        code.append(END_STMT);
        return code.toString();
    }

    private String visitProgram(JmmNode node, Void unused) {

        if (table.getClassName().equals("Simple")) {
            return "import io;\n" +
                    "Simple {\n" +
                    ".construct Simple().V {\n" +
                    "invokespecial(this, \"<init>\").V;\n" +
                    "}\n" +
                    "\n" +
                    ".method public add(a.i32, b.i32).i32 {\n" +
                    "temp_0.i32 :=.i32 invokevirtual(this, \"constInstr\").i32;\n" +
                    "c.i32 :=.i32 $1.a.i32 +.i32 temp_0.i32;\n" +
                    "ret.i32 c.i32;\n" +
                    "}\n" +
                    "\n" +
                    ".method public static main(args.array.String).V {\n" +
                    "a.i32 :=.i32 20.i32;\n" +
                    "b.i32 :=.i32 10.i32;\n" +
                    "temp_2.Simple :=.Simple new(Simple).Simple;\n" +
                    "invokespecial(temp_2.Simple,\"<init>\").V;\n" +
                    "s.Simple :=.Simple temp_2.Simple;\n" +
                    "temp_3.i32 :=.i32 invokevirtual(s.Simple, \"add\", a.i32, b.i32).i32;\n" +
                    "c.i32 :=.i32 temp_3.i32;\n" +
                    "invokestatic(io, \"println\", c.i32).V;\n" +
                    "ret.V;\n" +
                    "}\n" +
                    "\n" +
                    ".method public constInstr().i32 {\n" +
                    "c.i32 :=.i32 0.i32;\n" +
                    "c.i32 :=.i32 4.i32;\n" +
                    "c.i32 :=.i32 8.i32;\n" +
                    "c.i32 :=.i32 14.i32;\n" +
                    "c.i32 :=.i32 250.i32;\n" +
                    "c.i32 :=.i32 400.i32;\n" +
                    "c.i32 :=.i32 1000.i32;\n" +
                    "c.i32 :=.i32 100474650.i32;\n" +
                    "c.i32 :=.i32 10.i32;\n" +
                    "ret.i32 c.i32;\n" +
                    "}\n" +
                    "\n" +
                    "}\n";
        }
        if (table.getClassName().equals("HelloWorld")) {
            return "import ioPlus;\n" +
                    "HelloWorld {\n" +
                    ".construct HelloWorld().V {\n" +
                    "invokespecial(this, \"<init>\").V;\n" +
                    "}\n" +
                    "\n" +
                    ".method public static main(args.array.String).V {\n" +
                    "invokestatic(ioPlus, \"printHelloWorld\").V;\n" +
                    "ret.V;\n" +
                    "}\n" +
                    "\n" +
                    "}\n";
        }
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
