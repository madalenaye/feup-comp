package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.*;
import java.util.stream.Collectors;

import static pt.up.fe.comp2024.ast.Kind.*;

public class JmmSymbolTableBuilder {


    public static JmmSymbolTable build(JmmNode root) {

        List<String> imports = buildImports(root);

        JmmNode classDecl = root.getObject("classD", JmmNode.class);
        SpecsCheck.checkArgument(Kind.CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);
        String className = classDecl.get("name");
        String superclass = classDecl.hasAttribute("hyper") ? classDecl.get("hyper") : "";

        List<Symbol> fields = buildFields(classDecl);
        List<String> methods = buildMethods(classDecl);
        Map<String, Type> returnTypes = buildReturnTypes(classDecl);
        Map<String, List<Symbol>> params = buildParams(classDecl);
        Map<String, List<Symbol>> locals = buildLocals(classDecl);

        return new JmmSymbolTable(className, imports, methods, returnTypes, params, locals, superclass, fields);
    }

    private static List<String> buildImports(JmmNode root) {
        return root.getChildren(IMPORT_DECL).stream()
                .map(_import -> String.join(".", _import.getObjectAsList("name", String.class)))
                .toList();
    }

    private static List<Symbol> buildFields(JmmNode classDecl) {
        List<JmmNode> fieldNodes = classDecl.getChildren(VAR_DECL);
        return getList(fieldNodes);
    }

    private static List<String> buildMethods(JmmNode classDecl) {
        return classDecl.getChildren(METHOD_DECL).stream()
                .map(method -> method.get("name"))
                .toList();
    }

    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        Map<String, Type> map = new HashMap<>();

        List<JmmNode> methods = classDecl.getChildren(METHOD_DECL);

        for (JmmNode method : methods) {
            String methodName = method.get("name");
            map.put(methodName, buildType(method));
        }
        return map;
    }

    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {

        Map<String, List<Symbol>> map = new HashMap<>();
        List<JmmNode> methods = classDecl.getChildren(METHOD_DECL);

        for (JmmNode method : methods) {
            String methodName = method.get("name");
            List<JmmNode> params = method.getChildren(PARAM);
            map.put(methodName, getList(params));
        }
        return map;
    }

    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {

        Map<String, List<Symbol>> map = new HashMap<>();
        List<JmmNode> methods = classDecl.getChildren(METHOD_DECL);
        for (JmmNode method : methods){
            String methodName = method.get("name");
            List<JmmNode> localVars = method.getChildren(VAR_DECL);
            map.put(methodName, getList(localVars));
        }
        return map;
    }


    // aux functions
    private static List<Symbol> getList(List<JmmNode> vars) {
        List<Symbol> list = new ArrayList<>();
        for (JmmNode var : vars){
           list.add(buildSymbol(var));
        }
        return list;
    }

    private static Symbol buildSymbol(JmmNode var) {
        String name = var.get("name");
        return new Symbol(buildType(var), name);
    }

    private static Type buildType(JmmNode var) {
        JmmNode type = var.getJmmChild(0);
        return new Type(type.get("name"), isArray(type.get("isArray")));
    }

    private static boolean isArray(String s){
        return Boolean.parseBoolean(s);
    }

}
