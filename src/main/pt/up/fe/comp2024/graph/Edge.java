package pt.up.fe.comp2024.graph;

public class Edge {
    private Vertex src;
    private Vertex dest;

    public Edge(Vertex src, Vertex dest) {
        this.src = src;
        this.dest = dest;
    }
    
    public Vertex getSrc() {
        return this.src;
    }

    public Vertex getDest() {
        return this.dest;
    }
}
