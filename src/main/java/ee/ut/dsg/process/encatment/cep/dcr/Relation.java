package ee.ut.dsg.process.encatment.cep.dcr;

import java.util.Objects;

public class Relation {

    private Event source;
    private Event destination;

    private RelationType relationType;

    public Relation(Event source, Event destination, RelationType relationType) {
        this.source = source;
        this.destination = destination;
        this.relationType = relationType;
    }

    public Event getSource() {
        return source;
    }

    public Event getDestination() {
        return destination;
    }

    public RelationType getRelationType() {
        return relationType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Relation relation = (Relation) o;
        return source.equals(relation.getSource()) && destination.equals(relation.getDestination()) && relationType == relation.getRelationType();
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, destination, relationType);
    }
}
