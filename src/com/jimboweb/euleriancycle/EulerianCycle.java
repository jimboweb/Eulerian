package com.jimboweb.euleriancycle;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author jimstewart
 */
public class EulerianCycle {

    public int isGraphEvenOps;
    public int growCycleOps;
    public int startNewCycleOps;
    public int buildGraphOps;
    public int addOrModifyNodesOps;

    public static void main(String[] args) {
        new Thread(null, new Runnable() {
            public void run() {
                try {
                    new EulerianCycle().run();
                } catch (IOException ex) {
                    Logger.getLogger(EulerianCycle.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }, "1", 1 << 26).start();
    }

    public void run() throws IOException {
        Scanner scanner = new Scanner(System.in);
        ArrayList<int[]> inputs = new ArrayList<>();
        int[] in = new int[2];
        in[0] = scanner.nextInt();
        in[1] = scanner.nextInt();
        if(in[0]==0||in[1]==0){
            System.out.println("0");
            System.exit(0);
        }
        inputs.add(in);
        int n = inputs.get(0)[1];
        for (int i = 0; i < n; i++) {
            in = new int[2];
            in[0] = scanner.nextInt();
            in[1] = scanner.nextInt();
            inputs.add(in);
        }
        Graph g = buildGraph(inputs);
        Cycle c;
        if(!g.isGraphEven()){
            c = new Cycle(0);
        } else {
            c = g.makeEulerianCycle();
        }
        if(c.edges.size()==0){
            //either graph is even or couldn't find edge some other reason
            System.out.println("0");
        } else {
            System.out.println("1");
            for (int i : c.outputAsArray(g)) {
                System.out.print(i + " ");
            }
            System.out.println();
        }

    }

    class Graph{
        private Edge[] edges;
        private Node[] nodes;
        int nodeNum;
        int edgeNum;
        public Graph(int n, int m){
            edges = new Edge[m];
            nodes = new Node[n];
            this.nodeNum = n;
            this.edgeNum = m;
        }
        public Edge addEdge(int ind, int from, int to){
            Edge e = new Edge(from, to);
            e.nodeNum = ind;
            edges[ind] = e;
            return e;
        }

        public void addEdgeToCycle(int edgeGraphIndex, int edgeNodeIndex, Cycle c, boolean edgeIsOpen){ //,boolean edgeIsOpen;
            if(edgeIsOpen){
                c.addOpenEdge(c.edges.size());
            }
            c.addEdge(edgeGraphIndex);
            setNodeUsedByEdgeAndNodeIndex(edgeGraphIndex,edgeNodeIndex);
        }

        public void setNodeUsedByEdgeAndNodeIndex(int edgeGraphIndex, int edgeNodeIndex){
            nodes[edges[edgeGraphIndex].from()].setEdgeUsed(edgeNodeIndex);
        }

        public ArrayList<Integer> unusedEdgesFromEdge(int e){
            return nodes[edges[e].to()].unusedEdgesOut;
        }
        public ArrayList<Integer> edgesFromEdge(int e){
            return nodes[edges[e].to()].edgesOut;
        }
        public ArrayList<Integer> edgesToEdge(int e){
            return nodes[edges[e].from()].edgesIn;

        }
        public int size(){
            return edges.length;
        }
        public Edge getEdge(int n){
            return edges[n];
        }

        public boolean isGraphEven(){
            for(Node n:nodes){
                isGraphEvenOps++;
                if (n.edgesIn.size()!=n.edgesOut.size()){
                    return false;
                }
            }
            return true;
        }
        public boolean edgeHasOpenEdges(int e){
            return nodes[edges[e].to()].hasOpenEdges();
        }


        public Cycle makeEulerianCycle(){
            Cycle newCycle;
            Cycle oldCycle = new Cycle(edges.length);
            //find new node to start from
            do{
                newCycle= new Cycle(edges.length);

                newCycle = oldCycle.startNewCycle(newCycle,this);
                if(newCycle.edges.size()==0){
                    //return empty cycle because couldn't find available edge
                    return new Cycle(0);
                }
                newCycle = growCycle(newCycle);
                newCycle.appendCycle(oldCycle);
                oldCycle = newCycle.copy();
            } while (newCycle.edges.size() < edges.length);
            return newCycle;
        }

        /**
         * add new edges to the cycle from before
         * @param newCycle
         * @return
         */
        Cycle growCycle(Cycle newCycle){
            int nextEdge = newCycle.getFirstEdgeOfNextCycle();
            int firstEdge = nextEdge;
            while (edges[nextEdge].to!=edges[firstEdge].from){
                ArrayList<Integer> unusedEdges = unusedEdgesFromEdge(nextEdge);
                int nextEdgeGraphIndex = unusedEdges.get(0);
                nextEdge = nextEdgeGraphIndex;
                addEdgeToCycle(nextEdge,0,newCycle, edgeHasOpenEdges(nextEdge));
                growCycleOps++;//debug
            }

            return newCycle;
        }

    }


    class Edge{
        private int from;
        private int to;
        private int nodeNum;
        public Edge(int from, int to){
            this.from = from;
            this.to = to;
        }
        private int from(){
            return from;
        }
        private int to(){
            return to;
        }
        public int getFromNode(){
            return from;
        }
        public int getToNode(){
            return to;
        }
    }

    class Node{
        private ArrayList<Integer> edgesIn;
        private ArrayList<Integer> edgesOut;
        private ArrayList<Integer> unusedEdgesOut;
        private int nodeNum = -1;
        public Node(int nodeNum){
            edgesIn = new ArrayList<>();
            edgesOut = new ArrayList<>();
            unusedEdgesOut = new ArrayList<>();
            this.nodeNum = nodeNum;
        }
        public void addEdgeIn(int n){
            edgesIn.add(n);
        }
        public void addEdgeOut(int n){
            unusedEdgesOut.add(n);
            edgesOut.add(n);
        }
        public void setEdgeUsed(int i){
            unusedEdgesOut.remove(i);
        }
        public boolean hasOpenEdges(){
            return unusedEdgesOut.size()>0;
        }
    }

    class Cycle{
        private ArrayList<Integer> edges;
        private boolean[] visited;
        private int graphSize;
        // TODO: 0: 1/28/18 have to change this to openEdgesLocalIndex
        // every time I add to or get from it use local index
        private Stack<Integer> openEdgesLocalIndex;
        public Cycle(int graphSize){
            edges = new ArrayList<>();
            visited = new boolean[graphSize];
            this.graphSize=graphSize;
            openEdgesLocalIndex = new Stack<>();
        }
        //newCyclePreviousEdge is the LOCAL INDEX IN THIS CYCLE of the edge the next cycle starts from
        public Integer newCyclePreviousEdge;


        /**
         * this starts a new cycle, setting its firstEdge and newCyclePreviousEdge
         * @param newCycle the new cycle, should be empty
         * @param gr the graph
         * @return a new cycle with correct firstEdge and newCyclePreviousEdge
         */
        private Cycle startNewCycle(Cycle newCycle, Graph gr){
            if(size()==0){
                gr.addEdgeToCycle(0,0,newCycle, gr.edgeHasOpenEdges(0));
                newCycle.setNewCyclePreviousEdge(-1);
                return newCycle;
            }
            int lastOpenEdge;
            int lastOpenEdgeGraphIndex;
            int lastOpenEdgeCycleIndex;
            do{
                // TODO: 1/28/18 make this use local cycle reference
                lastOpenEdgeCycleIndex = getLastOpenEdge();
                lastOpenEdgeGraphIndex = edges.get(lastOpenEdgeCycleIndex);
                startNewCycleOps++;
            } while (gr.unusedEdgesFromEdge(lastOpenEdgeGraphIndex).size()<1);
            int firstEdge = gr.unusedEdgesFromEdge(lastOpenEdgeGraphIndex).get(0);
            newCycle.setNewCyclePreviousEdge(lastOpenEdgeCycleIndex);
            gr.addEdgeToCycle(firstEdge,0,newCycle,gr.edgeHasOpenEdges(firstEdge));
            return newCycle;
        }



        /**
         * this appends the other cycle starting from newCyclePreviousEdge and then wrapping around
         * @param otherCycle
         */
        public void appendCycle(Cycle otherCycle){
            int prevEdge = getNewCyclePreviousEdge();
            if (prevEdge >= 0 && otherCycle.size()>prevEdge) {
                List<Integer> appendCycle = otherCycle.edges.subList(prevEdge+1,otherCycle.edges.size());
                appendCycle.addAll(otherCycle.edges.subList(0,prevEdge+1));
                edges.addAll(appendCycle);
                // FIXME: 2/2/18 getting same edge twice
                addAllOpenEdges(otherCycle.getopenEdgesLocalIndex());
            }
        }

        public void setopenEdgesLocalIndex(Stack<Integer> openEdgesLocalIndex) {
            this.openEdgesLocalIndex = openEdgesLocalIndex;
        }

        public Stack<Integer> getopenEdgesLocalIndex() {
            return openEdgesLocalIndex;
        }

        public Integer getNewCyclePreviousEdge() {
            return newCyclePreviousEdge;
        }

        public void setNewCyclePreviousEdge(Integer newCyclePreviousEdge) {
            this.newCyclePreviousEdge = newCyclePreviousEdge;
        }

        public void addOpenEdge(int e){
            openEdgesLocalIndex.push(e);
        }

        public void addAllOpenEdges(Stack<Integer> otherOpenEdges){
            openEdgesLocalIndex.addAll(otherOpenEdges);
        }

        public int getLastOpenEdge(){
            return openEdgesLocalIndex.pop();
        }

        public void addEdge(int e){
            edges.add(e);
            visited[e] = true;
        }
        public int size(){
            return edges.size();
        }
        public int getEdge(int n){
            return edges.get(n);
        }
        public int getFirstEdgeOfNextCycle(){
            if (edges.size()>0) {
                return edges.get(0);
            }
            return -1;
        }
        public int getLastEdge(){return edges.get(edges.size()-1);}
        public Cycle copy(){
            Cycle c = new Cycle(graphSize);
            c.edges = new ArrayList<>(edges);
            Collections.copy(this.edges,c.edges);
            c.setopenEdgesLocalIndex(getopenEdgesLocalIndex());
            return c;
        }
        public int[] outputAsArray(Graph graph){
            int[] rtrn = new int[edges.size()];
            for(int i=0;i<edges.size();i++){
                rtrn[i] = graph.edges[edges.get(i)].from+1;
            }
            return rtrn;
        }


        @Override
        public String toString() {
            String rtrnString = "Cycle: ";
            for(Integer e:edges){
                rtrnString += e + " ";
            }
            return rtrnString;
        }

    }


    public Graph buildGraph(ArrayList<int[]> inputs){
        int n = inputs.get(0)[0];
        int m = inputs.get(0)[1];
        ArrayList<ArrayList<Integer>> edgesFromNode = new ArrayList<>();
        for(int i=0;i<n;i++){
            edgesFromNode.add(new ArrayList<>());
        }
        ArrayList<ArrayList<Integer>> edgesToNode = new ArrayList<>();
        for(int i=0;i<n;i++){
            edgesToNode.add(new ArrayList<>());
        }
        Graph g = new Graph(n,m);
        for(int i=1;i<inputs.size();i++){
            int x=inputs.get(i)[0];
            int y=inputs.get(i)[1];
            int from = x-1;
            int to = y-1;
            int edgeInd = i-1;
            Edge e = g.addEdge(edgeInd, from, to);
            edgesFromNode.get(from).add(edgeInd);
            edgesToNode.get(to).add(edgeInd);
            buildGraphOps++; //debug
            g.nodes = addOrModifyNodes(g.nodes,edgeInd,from,to);
        }
        return g;
    }

    private Node[] addOrModifyNodes(Node[] nodes, int edgeNum, int from, int to){
        Node nextNode;
        addOrModifyNodesOps++; //debug
        if(nodes[from]!=null){
            nextNode = nodes[from];
        } else {
            nextNode = new Node(from);
            nodes[from] = nextNode;
        }
        if(!nextNode.edgesOut.contains(edgeNum)){
            nextNode.addEdgeOut(edgeNum);
        }

        if(nodes[to]!=null){
            nextNode = nodes[to];
        } else {
            nextNode = new Node(to);
            nodes[to]=nextNode;
        }
        if(!nextNode.edgesIn.contains(edgeNum)){
            nextNode.addEdgeIn(edgeNum);
        }
        return nodes;
    }

}
