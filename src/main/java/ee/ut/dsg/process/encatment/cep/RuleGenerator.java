package ee.ut.dsg.process.encatment.cep;

public abstract class RuleGenerator {

    public static final String COMPLETED = "\"completed\"";
    public static final String STARTED = "\"started\"";
    public static final String SKIPPED = "\"skipped\"";

    protected void createContext(StringBuilder sb) {
        sb.append("//create a context\n" +
                "create context partitionedByPmIDAndCaseID partition by pmID, caseID from ProcessEvent;\n" +
                "\n" +
                "@Audit\n" +
                "@name('track-events') context partitionedByPmIDAndCaseID select  pmID, caseID, nodeID, cycleNum, state, payLoad, timestamp  from ProcessEvent;\n");
    }

    public abstract String generateEPLModule();
}
