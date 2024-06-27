package ee.ut.dsg.process.encatment.cep.transition;



import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class TransitionGraph {

    private Node initialState;
    private Set<Node> states;
    private Set<Transition> transitions;

    public TransitionGraph()
    {
        states = new HashSet<>();
        transitions = new HashSet<>();
    }
    public void setInitialState(Node initialState) {
        this.initialState = initialState;
    }

    public boolean addNoe(Node n)
    {
        return states.add(n);
    }

    public void addEdge(Node src, Node target, String label)
    {
        addNoe(src);
        addNoe(target);
        Transition t = new Transition(src,target,label);
        transitions.add(t);
    }
    public Node getInitialState()
    {
        return initialState;
    }

    public Node getNodeByID(String id)
    {
        for (Node n : this.states)
            if (n.getId().equals(id))
                return n;
        return null;
    }
    public Set<Node> getNextNodes(Node current)
    {
        Set<Node> result = new HashSet<>();
        for (Transition t : transitions)
            if (t.getSource().equals(current))
                result.add(t.getTarget());
        return result;
    }

    public static TransitionGraph parseDCRSVGToTransitionGraph(String svgFilePath) throws IOException {
        TransitionGraph tg = new TransitionGraph();
        try (BufferedReader br = new BufferedReader(new FileReader(svgFilePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.startsWith("<!") && line.contains(("&"))) // this is an edge
                {
                    String[] nodeIDs = line.split("&#45;&gt;");
                    Node src = tg.getNodeByID(nodeIDs[0].replace("<!--","").trim());
                    Node target = tg.getNodeByID(nodeIDs[1].replace("-->","").trim());
                    if (src != null && target != null)
                        tg.addEdge(src,target,"");
                    if (src == null)
                    {
                        System.out.println(String.format("Source not found:%s", nodeIDs[0].replace("<!--","").trim() ));
                    }
                    if (target == null)
                    {
                        System.out.println(String.format("Target not found:%s", nodeIDs[1].replace("-->","").trim()));
                    }
                }
                else if (line.startsWith("<!") && !line.contains(("&"))) // this is a node
                {

                    String nodeID = line.replace("<!--", "").replace("-->", "").trim();
                    Node.NodeType nt=null;
                    String nextLine = br.readLine();
                    while (nextLine != null && !nextLine.startsWith("<text")) {
                        if (nextLine.contains("polygon"))
                            nt = Node.NodeType.Action;
                        nextLine = br.readLine();
                    }
                    String nodeLabel = "";
                    while (nextLine != null && nextLine.startsWith("<text")) {
                        int pos1 = nextLine.indexOf(">");
                        int pos2 = nextLine.indexOf("</text");
                        nodeLabel += nextLine.substring(pos1+1,pos2) + "\n";
//                        nodeLabel += nextLine.replaceAll("<text\\w*>", "").replace("</text>", "") + "\n";
                        nextLine = br.readLine();
                    }

                    Node n = new Node(nodeLabel, nodeID, nodeLabel.equals("x")? Node.NodeType.X : nt == null? Node.NodeType.State: Node.NodeType.Action );
                    tg.addNoe(n);
                }
                else
                    continue;

//                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        tg.print();
        return tg;
    }

    public void print() {
        System.out.println(states.toString());
    }
}
