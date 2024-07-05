package ee.ut.dsg.process.encatment.cep.dcr;


import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;

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

    public boolean isEmpty()
    {
        return nodes.isEmpty();
    }
    public Iterator<Event> getNodes()
    {
        return nodes.iterator();
    }

    public Iterator<Relation> getEdges()
    {
        return edges.iterator();
    }

    public Set<Event> getPreConditionNodes(Event e)
    {
        return edges.stream().filter(r ->
                r.getDestination().equals(e) && (r.getRelationType() == RelationType.CONDITION || r.getRelationType() == RelationType.PRE_Condition)).map(Relation::getSource).collect(Collectors.toSet());
    }

    public Set<Event> getMileStones(Event e) {
        return edges.stream().filter(r ->
                r.getDestination().equals(e) && (r.getRelationType() == RelationType.MILESTONE)).map(Relation::getSource).collect(Collectors.toSet());
    }
}
