package ee.ut.dsg.process.encatment.cep.transition;

public class Node {

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

    private String name;
    private String id;

    public Node(String n, String id, NodeType nt)
    {
        this.name = n;
        this.id  = id;
        this.nodeType = nt;
    }

    public enum NodeType{
        X,
        Action,
        State
    }
    private NodeType nodeType;
    public boolean equals(Object other)
    {
        if (other instanceof Node)
        {
            Node oNode = (Node) other;
            return oNode.getId().equals(this.getId());
        }
        return false;
    }

    public String toString()
    {
        return String.format("Id:%s, Name:%s\n", this.getId(),this.getName());
    }
}
