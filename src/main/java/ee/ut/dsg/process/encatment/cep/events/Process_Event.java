package ee.ut.dsg.process.encatment.cep.events;

import java.util.Map;

public class Process_Event {
    private int pmID;
    private int caseID;
    private String nodeID;
    private int cycleNum;
    private String state;
    private Map<String, Object> payLoad;
    private long timestamp;

    public Process_Event(int PM_ID, int caseID, String node_ID, int CYCLE_NUM, String state, Map<String, Object> pay_Load, long timestamp) {
        this.pmID = PM_ID;
        this.caseID = caseID;
        nodeID = node_ID;
        this.cycleNum = CYCLE_NUM;
        this.state = state;
        payLoad = pay_Load;
        this.timestamp = timestamp;
    }

    public int getPmID() {
        return pmID;
    }

    public int getCaseID() {
        return caseID;
    }

    public String getNodeID() {
        return nodeID;
    }

    public int getCycleNum() {
        return cycleNum;
    }

    public String getState() {
        return state;
    }

    public Map<String, Object> getPayLoad() {
        return payLoad;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
