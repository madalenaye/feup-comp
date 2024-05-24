package pt.up.fe.comp2024.optimization;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;
import pt.up.fe.comp2024.CompilerConfig;
import pt.up.fe.comp2024.graph.Graph;

import java.util.*;

import static pt.up.fe.comp2024.CompilerConfig.getRegisterAllocation;

public class JmmOptimizationImpl implements JmmOptimization {

    @Override
    public OllirResult toOllir(JmmSemanticsResult semanticsResult) {

        // convert vararg types
        VarargsConverter varargsConverter = new VarargsConverter();
        varargsConverter.visit(semanticsResult.getRootNode(), semanticsResult.getSymbolTable());

        var visitor = new OllirGeneratorVisitor(semanticsResult.getSymbolTable());
        var ollirCode = visitor.visit(semanticsResult.getRootNode());

        return new OllirResult(semanticsResult, ollirCode, Collections.emptyList());
    }

    @Override
    public JmmSemanticsResult optimize(JmmSemanticsResult semanticsResult) {
        if (!CompilerConfig.getOptimize(semanticsResult.getConfig())) {
            return semanticsResult;
        }

        boolean hasFolded, hasPropagated, canBeOptimized = true;
        ConstFoldVisitor constFoldVisitor = new ConstFoldVisitor();
        ConstPropagationVisitor constPropagationVisitor = new ConstPropagationVisitor();
        var table = semanticsResult.getSymbolTable();

        while (canBeOptimized) {
            var root = semanticsResult.getRootNode();

            hasFolded = constFoldVisitor.visit(root);
            hasPropagated = constPropagationVisitor.visit(root, table);

            canBeOptimized = hasFolded || hasPropagated;
        }

        String res = semanticsResult.getRootNode().toTree();
        System.out.println(res);
        return semanticsResult;
    }

    @Override
    public OllirResult optimize(OllirResult ollirResult) {
        
        int n = getRegisterAllocation(ollirResult.getConfig());
        if (n == -1)  {
            return ollirResult;
        }

        ollirResult.getOllirClass().buildCFGs();

        List<Method> methods = ollirResult.getOllirClass().getMethods();
        for (Method method : methods) {
            var interferences = livenessAnalysis(method);
            Graph graph = new Graph(method.getVarTable(), interferences, method.getMethodName());
            int min = graph.minRegisters();
            if (n == 0 || n >= min) {
                graph.allocateRegisters(min);
                graph.reportMapping(ollirResult, method.getMethodName());
            } else {
                String message = String.format("Can't allocate %d registers, minimum %d required", n, min);
                ollirResult.getReports().add(Report.newError(
                        Stage.OPTIMIZATION,
                        1,1,
                        message,
                        null));
                return ollirResult;
            }

        }

        return ollirResult;
    }

    private HashMap<Node, HashSet<String>> defs(List<Instruction> instructions) {
        HashMap<Node, HashSet<String>> map = new HashMap<>();
        for (Instruction instruction : instructions) {
            map.put(instruction, def(instruction));
        }
        return map;
    }
    private HashSet<String> def(Instruction instruction) {
        HashSet<String> set = new HashSet<>();
        if (instruction instanceof AssignInstruction assignInstruction) {
            Operand op = (Operand) assignInstruction.getDest();
            set.add(op.getName());
        }
        return set;
    }
    private List<HashSet<String>> livenessAnalysis(Method method) {
        var nodes = method.getInstructions();

        HashMap<Node, HashSet<String>> defs = defs(nodes);
        HashMap<Node, HashSet<String>> uses = UseUtils.uses(nodes);
        HashMap<Node, HashSet<String>> in = new HashMap<>();
        HashMap<Node, HashSet<String>> out = new HashMap<>();

        for (Node n : nodes) {
            in.put(n, new HashSet<>());
            out.put(n, new HashSet<>());
        }

        HashMap<Node, HashSet<String>> tmpIn = new HashMap<>(in);
        HashMap<Node, HashSet<String>> tmpOut = new HashMap<>(out);

        do {
            for (Node n : nodes) {
                tmpIn.replace(n, in.get(n));
                tmpOut.replace(n, out.get(n));

                // in(n) = use(n) ∪ (out(n) - def(n))
                var newIn = new HashSet<>(out.get(n));
                newIn.removeAll(defs.get(n));
                newIn.addAll(uses.get(n));
                in.replace(n, newIn);

                // out(n) = ∪ in(s), ∀ s ∈ succ(n)
                HashSet<String> newOut = new HashSet<>();
                for (var suc : n.getSuccessors()) {
                    if (suc.getNodeType().toString().equals("END")) {
                        break;
                    }
                    newOut.addAll(in.get(suc));
                }
                out.replace(n, newOut);
            }
        } while (!out.equals(tmpOut) || !in.equals(tmpIn));

        List<HashSet<String>> interferences = new ArrayList<>();

        for (Node n : nodes) {
            out.get(n).addAll(defs.get(n));
            if (in.get(n).size() > 1) {
                interferences.add(in.get(n));
            }
            if (out.get(n).size() > 1) {
                interferences.add(out.get(n));
            }
        }

       return interferences;

    }
}
