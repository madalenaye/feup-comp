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
        String superclass = "";
        if (classDecl.hasAttribute("hyper")){
           superclass = classDecl.get("hyper");
        }

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

        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), new Type(TypeUtils.getIntTypeName(), false)));

        return map;
    }

    private static Map<String, List<Symbol>> buildParams(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, List<Symbol>> map = new HashMap<>();
        return map;
        /*
        var intType = new Type(TypeUtils.getIntTypeName(), false);

        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), Arrays.asList(new Symbol(intType, method.getJmmChild(1).get("name")))));

        return map;*/
    }


    private static Map<String, List<Symbol>> buildLocals(JmmNode classDecl) {
        // TODO: Simple implementation that needs to be expanded

        Map<String, List<Symbol>> map = new HashMap<>();


        classDecl.getChildren(METHOD_DECL).stream()
                .forEach(method -> map.put(method.get("name"), getLocalsList(method)));

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
        // TODO: Simple implementation that needs to be expanded

        var intType = new Type(TypeUtils.getIntTypeName(), false);

        return methodDecl.getChildren(VAR_DECL).stream()
                .map(varDecl -> new Symbol(intType, varDecl.get("name")))
                .toList();
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
