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

    public void allocateRegister(int min, int max) {

        boolean[] usedRegs = new boolean[max - min + 1];

        for (Edge edge : edges) {
            int reg = edge.getDest().getDescriptor().getVirtualReg();
            if (reg >= min && reg <= max) {
                usedRegs[reg - min] = true;
            }
        }

        for (int i = 0; i < usedRegs.length; i++) {
            if (!usedRegs[i]) {
                this.descriptor.setVirtualReg(min + i);
                return;
            }
        }
    }
}
