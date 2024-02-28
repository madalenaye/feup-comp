package pt.up.fe.comp2024.symboltable;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.SpecsCheck;

import java.util.*;

import static pt.up.fe.comp2024.ast.Kind.*;

public class JmmSymbolTableBuilder {


    public static JmmSymbolTable build(JmmNode root) {

        var imports = buildImports(root);

        var classDecl = root.getObject("classD", JmmNode.class);
        SpecsCheck.checkArgument(Kind.CLASS_DECL.check(classDecl), () -> "Expected a class declaration: " + classDecl);
        String className = classDecl.get("name");
        String superclass = classDecl.hasAttribute("hyper") ? classDecl.get("hyper") : "";

        var fields = buildFields(classDecl);
        var methods = buildMethods(classDecl);
        var returnTypes = buildReturnTypes(classDecl);
        var params = buildParams(classDecl);
        var locals = buildLocals(classDecl);

        return new JmmSymbolTable(className, imports, methods, returnTypes, params, locals, superclass, fields);
    }

    private static Map<String, Type> buildReturnTypes(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, Type> map = new HashMap<>();

        var methods = classDecl.getChildren(METHOD_DECL);
        for ( JmmNode method : methods) {
                String methodName = method.get("name");
                JmmNode type = method.getJmmChild(0);
                String typeName = type.get("name");
                boolean isArray = Boolean.parseBoolean(type.get("isArray"));
                map.put(methodName, new Type(typeName, isArray));
        }
        return map;
    }

    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {

        Map<String, List<Symbol>> map = new HashMap<>();
        var methods = classDecl.getChildren(METHOD_DECL);

        for (JmmNode method : methods) {
            List<Symbol> list = new ArrayList<>();
            String methodName = method.get("name");
            var params = method.getChildren(PARAM);
            for (JmmNode param : params) {
                String paramName = param.get("name");
                JmmNode type = param.getJmmChild(0);
                String typeName = type.get("name");
                list.add(new Symbol(new Type(typeName, false), paramName));
            }
            map.put(methodName, list);
        }
        return map;
    }


    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, List<Symbol>> map = new HashMap<>();
        var methods = classDecl.getChildren(METHOD_DECL);
        for (JmmNode method : methods){
            List<Symbol> list = new ArrayList<>();
            String methodName = method.get("name");
            var localVars = method.getChildren(VAR_DECL);
            for (JmmNode localVar : localVars){
                String localVarName = localVar.get("name");
                var type = localVar.getJmmChild(0);
                String typeName = type.get("name");
                list.add(new Symbol(new Type(typeName, false), localVarName));
            }
            map.put(methodName, list);
        }
        return map;
    }

    private static List<String> buildImports(JmmNode root) {
        return root.getChildren(IMPORT_DECL).stream()
                .map(_import -> _import.get("name"))
                .toList();
    }
    private static List<String> buildMethods(JmmNode classDecl) {
       return classDecl.getChildren(METHOD_DECL).stream()
                .map(method -> method.get("name"))
                .toList();
    }

    private static List<Symbol> getLocalsList(JmmNode methodDecl) {
        List<Symbol> list = new ArrayList<>();
        return list;
    }
    
    private static List<Symbol> buildFields(JmmNode classDecl) {
        var fieldNodes = classDecl.getChildren(VAR_DECL);
        List<Symbol> list = new ArrayList<>();
        for (JmmNode node : fieldNodes) {
            String field_name = node.get("name");
            JmmNode type = node.getJmmChild(0);
            String type_name = type.get("name");
            list.add(new Symbol(new Type(type_name, false), field_name));
        }
        return list;
    }

}
