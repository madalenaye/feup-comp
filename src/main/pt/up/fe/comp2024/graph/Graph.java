package pt.up.fe.comp2024.graph;

import org.specs.comp.ollir.Descriptor;
import org.specs.comp.ollir.VarScope;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.*;

public class Graph {

    private List<Vertex> localRegisters;
    private List<Vertex> fixedRegisters;
    private Stack<Vertex> stack;

    boolean isStatic;

    public Graph(HashMap<String, Descriptor> varTable, List<HashSet<String>> interferences, String methodName) {
        this.stack = new Stack<>();
        this.isStatic = methodName.equals("main");
        createVertices(varTable);
        addEdges(interferences);
    }

    private void createVertices(HashMap<String, Descriptor> varTable) {
        this.localRegisters = new ArrayList<>();
        this.fixedRegisters  = new ArrayList<>();

        boolean hasThis = false;
        for (String varName : varTable.keySet()) {

            Descriptor descriptor = varTable.get(varName);
            Vertex vertex = new Vertex(varName, descriptor);

            if (varName.equals("this")) {
                hasThis = true;
                fixedRegisters.add(0, vertex);
            }
            else if (descriptor.getScope().equals(VarScope.LOCAL)) {
                localRegisters.add(vertex);
            } else {
                fixedRegisters.add(vertex);
            }
        }
        if (!hasThis && !isStatic) {
            fixedRegisters.add(0, new Vertex("this", null));
        }
    }

    private void addEdges(List<HashSet<String>> interferences) {
        for (var interference : interferences) {
            addEdges(interference);
        }
    }

    private void addEdges(HashSet<String> interference) {
        for (String varName : interference) {
            Vertex src = getVertex(varName);
            if (src == null) continue;
            for (String destName : interference) {
                if (!varName.equals(destName) && !src.connectedTo(destName)) {
                    Vertex dest = getVertex(destName);
                    if (dest == null) continue;

                    connect(src, dest);
                }
            }
        }
    }

    private void connect(Vertex src, Vertex dest) {
        src.addEdge(new Edge(src, dest));
        dest.addEdge(new Edge(dest, src));
    }

    private Vertex getVertex(String varName) {
        for (var v : localRegisters) {
            if (v.getVariable().equals(varName)) {
                return v;
            }
        }
        return null;
    }

    private void removeVertex(Vertex vertex) {
        for (Edge edge : vertex.getCurrentEdges()) {
            edge.getDest().removeCurrentEdge(vertex);
        }
        vertex.getCurrentEdges().clear();
    }

    public boolean colorWithKColors(int k) {

        k -= fixedRegisters.size();
        stack.clear();

        List<Vertex> vertexes = new ArrayList<>(localRegisters);
        for (Vertex v : localRegisters) {
            v.setCurrentEdges(new ArrayList<>(v.getEdges()));
        }

        // Remove nodes that have degree < N
        var iterator = localRegisters.iterator();
        while(iterator.hasNext()) {
            Vertex vertex = iterator.next();
            if (vertex.getCurrentEdges().size() < k) {
                removeVertex(vertex);
                stack.push(vertex);
                iterator.remove();
                iterator = localRegisters.iterator();
            }
        }

        localRegisters = vertexes;
        return stack.size() == localRegisters.size();
    }

    public void allocateRegisters(int k) {

        colorWithKColors(k);

        for (Vertex vertex : localRegisters) {
            vertex.getDescriptor().setVirtualReg(-1);
        }

        while (!stack.empty()) {
            Vertex vertex = stack.pop();
            vertex.allocateRegister(fixedRegisters.size(), k - 1);
        }
    }

    public int minRegisters() {
        int min = fixedRegisters.size();
        int max = min + localRegisters.size();
        while (min < max) {
            int mid = min + (max - min) / 2;
            if (colorWithKColors(mid)) {
                max = mid;
            } else {
                min = mid + 1;
            }
        }
        return min;
    }

    public void reportMapping(OllirResult ollirResult, String methodName) {
        StringBuilder message = new StringBuilder();
        message.append("Method ").append(methodName).append(":\n");

        for (Vertex vertex : fixedRegisters) {
            if (vertex.getDescriptor() == null) {
                message.append(vertex.getVariable()).append(" -> register 0\n");
                continue;
            }
            message.append(vertex.getVariable()).append(" -> register ").append(vertex.getDescriptor().getVirtualReg()).append("\n");
        }

        for (Vertex vertex : localRegisters) {
            message.append(vertex.getVariable()).append(" -> register ").append(vertex.getDescriptor().getVirtualReg()).append("\n");
        }

        ollirResult.getReports().add(Report.newLog(
                Stage.OPTIMIZATION,
                1,1,
                message.toString(),
                null));
    }
}
