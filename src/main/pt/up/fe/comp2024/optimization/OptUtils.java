package pt.up.fe.comp2024.optimization;

import org.specs.comp.ollir.Instruction;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp2024.ast.Kind;
import pt.up.fe.comp2024.ast.NodeUtils;
import pt.up.fe.comp2024.ast.TypeUtils;
import pt.up.fe.specs.util.exceptions.NotImplementedException;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static pt.up.fe.comp2024.ast.Kind.TYPES;


//import static pt.up.fe.comp2024.ast.Kind.TYPE;

public class OptUtils {
    private static int tempNumber = -1;

    private static int ifNumber = 0;
    private static int whileNumber = 0;

    public static String getTemp() {

        return getTemp("tmp");
    }

    public static String getTemp(String prefix) {

        return prefix + getNextTempNum();
    }

    public static int getNextTempNum() {
        tempNumber += 1;
        return tempNumber;
    }

    public static int getNextIfNum() {
        ifNumber += 1;
        return ifNumber;
    }

    public static String getIf() {

        return "if" + getNextIfNum();
    }

    public static String getEndIf() {

        return "endif" + ifNumber;
    }

    public static int getNextWhileNum() {
        whileNumber += 1;
        return whileNumber;
    }

    public static String getWhileCond() {

        return "whileLoop" + getNextWhileNum();
    }

    public static String getWhileLoop() {

        return "whileCond" + whileNumber;
    }

    public static String getWhileEnd() {

        return "whileEnd" + whileNumber;
    }

    public static String toOllirBool(String boolValue) {

        if(Objects.equals(boolValue, "true")) return "1";
        else if (Objects.equals(boolValue, "false")) return "0";

        return null;
    }

    public static String toOllirType(JmmNode typeNode) {

        Kind.checkOrThrow(typeNode, TYPES);

        String typeName = typeNode.get("name");

        if (TypeUtils.isArray(typeNode)) {
            return ".array" + toOllirType(typeName);
        }

        return toOllirType(typeName);
    }

    public static String toOllirType(Type type) {
        if (type.isArray()) {
            return ".array" + toOllirType(type.getName());
        }
        return toOllirType(type.getName());
    }

    private static String toOllirType(String typeName) {

        String type = "." + switch (typeName) {
            case "int" -> "i32";
            case "boolean" -> "bool";
            case "void" -> "V";
            default -> typeName;
        };

        return type;
    }

}
