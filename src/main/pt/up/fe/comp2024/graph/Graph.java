package pt.up.fe.comp2024.graph;

import org.specs.comp.ollir.Descriptor;

import java.util.*;

public class Graph {

    private List<Vertex> vertices;

    public Graph(HashMap<String, Descriptor> varTable, List<HashSet<String>> interferences) {
        this.vertices = createVertices(varTable);
        addEdges(interferences);
    }

    public List<Vertex> getVertices() {
        return vertices;
    }

    private List<Vertex> createVertices(HashMap<String, Descriptor> varTable) {
        List<Vertex> vertices = new ArrayList<>();
        for (String varName : varTable.keySet()) {
            Descriptor descriptor = varTable.get(varName);
            Vertex vertex = new Vertex(varName, descriptor);
            vertices.add(vertex);
        }
        return vertices;
    }

    private void addEdges(List<HashSet<String>> interferences) {
        for (var interference : interferences) {
            addEdges(interference);
        }
    }

    private void addEdges(HashSet<String> interference) {
        for (String varName : interference) {
            Vertex src = getVertex(varName);
            for (String destName : interference) {
                if (!varName.equals(destName) && !src.connectedTo(destName)) {
                    Vertex dest = getVertex(destName);
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
        for (var v : vertices) {
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

    public Stack<Vertex> colorWithKColors(int k) {

        List<Vertex> vertexes = new ArrayList<>(vertices);
        for (Vertex v : vertices) {
            v.setCurrentEdges(new ArrayList<>(v.getEdges()));
        }

        k = 3;
        Stack<Vertex> stack = new Stack<>();

        // Remove nodes that have degree < N
        var iterator = vertices.iterator();
        while(iterator.hasNext()) {
            Vertex vertex = iterator.next();
            if (vertex.getCurrentEdges().size() < k) {
                removeVertex(vertex);
                stack.push(vertex);
                iterator.remove();
            }
        }

        vertices = vertexes;
        return stack;
    }


    public void allocateRegisters(int k, Stack<Vertex> stack) {
        k = 3;

        for (Vertex vertex : vertices) {
            vertex.getDescriptor().setVirtualReg(0);
        }

        while (!stack.empty()) {
            Vertex vertex = stack.pop();
            vertex.allocateRegister(k);
        }
    }
}
