package pt.up.fe.comp2024.graph;

import java.util.ArrayList;
import java.util.List;
import org.specs.comp.ollir.Descriptor;

public class Vertex {
    private String varName;
    private Descriptor descriptor;
    private List<Edge> edges;

    private List<Edge> currentEdges;

    public Vertex(String varName, Descriptor descriptor) {
        this.varName = varName;
        this.descriptor = descriptor;
        this.edges = new ArrayList<>();
        this.currentEdges = new ArrayList<>();
    }
    
    public String getVariable() {
        return this.varName;
    }

    public Descriptor getDescriptor() {
        return this.descriptor;
    }

    public List<Edge> getEdges() {
        return this.edges;
    }

    public List<Edge> getCurrentEdges() {
        return this.currentEdges;
    }

    public void setCurrentEdges(List<Edge> edges) {
        this.currentEdges = edges;
    }

    public void addEdge(Edge edge) {
        this.edges.add(edge);
    }

    public void removeCurrentEdge(Vertex vertex) {
        this.currentEdges.removeIf(edge -> edge.getDest().equals(vertex));
    }

    public boolean connectedTo(String dest) {
        for (Edge edge : this.edges) {
            if (edge.getDest().getVariable().equals(dest)) {
                return true;
            }
        }
        return false;
    }

    public void allocateRegister(int k) {
        int min = k;
        int max = 1;

        for (Edge edge : edges) {
            int reg = edge.getDest().getDescriptor().getVirtualReg();
            if (reg != 0) {
                min = Math.min(min, reg);
                max = Math.max(max, reg);
            }
        }

        if (min > 1) {
            this.descriptor.setVirtualReg(1);
        } else {
            this.descriptor.setVirtualReg(max+1);
        }
    }
}
