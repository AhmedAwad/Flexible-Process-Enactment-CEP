package ee.ut.dsg.process.encatment.cep.dcr;

import java.util.Objects;

public class Event {
    private boolean excluded;
    private String ID;
    private String name;

    public Event(boolean excluded, String ID, String name) {
        this.excluded = excluded;
        this.ID = ID;
        this.name = name;
    }

    public boolean isExcluded() {
        return excluded;
    }

    public void setExcluded(boolean newValue)
    {
        excluded = newValue;
    }

    public String getID() {
        return ID;
    }

    public void setName(String newName)
    {
        this.name = newName;
    }
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Event event = (Event) o;
        return ID.equals(event.getID());
    }

    @Override
    public int hashCode() {
        return Objects.hash(ID);
    }
}
