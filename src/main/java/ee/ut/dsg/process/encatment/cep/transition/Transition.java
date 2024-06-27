package ee.ut.dsg.process.encatment.cep.transition;

public class Transition {
    private Node source;
    private Node target;

    private String label;

    public Transition(Node source, Node target, String label) {
        this.source = source;
        this.target = target;
        this.label = label;
    }

    public Node getSource() {
        return source;
    }

    public Node getTarget() {
        return target;
    }

    public String getLabel() {
        return label;
    }
}
