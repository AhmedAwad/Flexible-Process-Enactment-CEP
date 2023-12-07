package ee.ut.dsg.process.encatment.cep.dcr;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class DCRGraph {

    private Set<Event> nodes;
    private Set<Relation> edges;

    public DCRGraph() {
        nodes = new HashSet<>();
        edges = new HashSet<>();
    }

    public boolean addNode(Event e)
    {
        return nodes.add(e);
    }

    public boolean addEdge(Relation edge)
    {
        return edges.add(edge);
    }

    public Event getNodeByID(String ID)
    {

        for (Event e: nodes)
            if (e.getID().equals(ID))
                return e;
        return null;
    }
}
