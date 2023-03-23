package ee.ut.dsg.process.encatment.cep;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.GatewayDirection;
import org.camunda.bpm.model.bpmn.impl.instance.*;
import org.camunda.bpm.model.bpmn.instance.DataOutput;
import org.camunda.bpm.model.bpmn.instance.DataOutputAssociation;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.xml.impl.type.ModelElementTypeImpl;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.camunda.bpm.model.xml.type.ModelElementType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RulesGenerator {

    private File bpmnModelFile;
    private BpmnModelInstance instance;
    public RulesGenerator(File file)
    {
        bpmnModelFile = file;
        instance = Bpmn.readModelFromFile(file);
    }

    public String generateEPLModule()
    {
        StringBuilder sb = new StringBuilder();

        //Create the context and the tracking of events
        sb.append("//create a context\n" +
                "create context partitionedByPmIDAndCaseID partition by pmID, caseID from ProcessEvent;\n" +
                "\n" +
                "@Audit\n" +
                "@name('track-events') context partitionedByPmIDAndCaseID select  pmID, caseID, nodeID, cycleNum, state, payLoad, timestamp  from ProcessEvent;\n");

        //Add the case variables table
        sb.append("//Create the table that holds case variables\n" +
                "context partitionedByPmIDAndCaseID create table Case_Variables (pmID int primary key, caseID int primary key, variables java.util.Map);\n");

        sb.append("context partitionedByPmIDAndCaseID\n" +
                "create expression boolean js:evaluate(caseVariables, cond) [\n" +
                "    evaluate(caseVariables, cond);\n" +
                "    function evaluate(caseVariables, cond){\n" +
                "        if (cond == \"true\")\n" +
                "        {\n" +
                "            return true;\n" +
                "        }\n" +
                "        if (cond == \"cond1\")\n" +
                "        {\n" +
                "            return caseVariables.get('cond1');\n" +
                "        }\n" +
                "        if (cond == \"cond2\")\n" +
                "        {\n" +
                "            return caseVariables.get('cond2');\n" +
                "        }\n" +
                "        if (cond == \"cond3\")\n" +
                "        {\n" +
                "            return caseVariables.get('cond3');\n" +
                "        }\n" +
                "        if (cond == \"cond4\")\n" +
                "        {\n" +
                "            return caseVariables.get('cond4');\n" +
                "        }\n" +
                "        return false;\n" +
                "    }\n" +
                "];\n");

        //Add the named window to track execution history per case
        sb.append("//History (named window)\n" +
                "context partitionedByPmIDAndCaseID create window Execution_History.win:keepall as  ProcessEvent;\n");
        for (FlowNode elem : instance.getModelElementsByType(FlowNode.class))
        {
            List<String> predecessors = elem.getPreviousNodes().list().stream().map(e -> "\"" +e.getName()+"\"").collect(Collectors.toList());
            String inList = predecessors.toString().replace("[", "(").replace("]", ")");
            if (elem instanceof org.camunda.bpm.model.bpmn.impl.instance.StartEventImpl)
            {
                sb.append("// Start event -- this shall be injected from outside\n" +
                        "context partitionedByPmIDAndCaseID \n" +
                        "insert into ProcessEvent(pmID, caseID, nodeID, cycleNum, state, payLoad, timestamp)\n" +
                        "select pred.pmID, Coalesce((select max(caseID)+1 from Execution_History where pmID = pred.pmID),1), \""+elem.getName()+"\",0,\"completed\", pred.payLoad, pred.timestamp\n" +
                        "from ProcessEvent(nodeID=\""+elem.getName()+"\", state=\"started\") as pred;\n" +
                        "//Inititate case variables as a response to the start event\n" +
                        "context partitionedByPmIDAndCaseID\n" +
                        "insert into Case_Variables (pmID, caseID, variables )\n" +
                        "select st.pmID, st.caseID, st.payLoad from ProcessEvent(nodeID=\""+elem.getName()+"\", state=\"completed\") as st;\n");
            }
            else {

                if (elem instanceof EndEventImpl)
                {
                    //Add events to the named window as long as there are no end events already in it.
                    sb.append("//End Event \n@Priority(10)\n" +
                            "@Name(\"Start-Event\") context partitionedByPmIDAndCaseID\n" +
                            "insert into Execution_History(pmID, caseID, nodeID, cycleNum, state, payLoad, timestamp)\n" +
                            "select event.pmID, event.caseID, event.nodeID, event.cycleNum, event.state, event.payLoad, event.timestamp from ProcessEvent as event\n" +
                            "where not exists (select 1 from Execution_History as H where H.pmID = event.pmID and H.caseID = event.caseID and\n" +
                            "      H.nodeID = \""+elem.getName()+"\" and H.state =\"completed\");\n");

                    //Generate a skipped or completed event based on the predecessor
                    sb.append("// End event\n" +
                            "@Priority(200) @Name('End-Event') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID, cycleNum, state, payLoad, timestamp)\n" +
                            "select pred.pmID, pred.caseID, \""+elem.getName()+"\", pred.cycleNum,\n" +
                            "case when pred.state=\"completed\" then \"completed\" else \"skipped\" end,\n" +
                            "CV.variables, pred.timestamp\n" +
                            "From ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID\n" +
                            "where pred.state in (\"completed\", \"skipped\") and pred.nodeID in "+ inList +"\n" +
                            "and not exists (select 1 from Execution_History as H where H.pmID = pred.pmID and H.caseID = pred.caseID and\n" +
                            "H.nodeID = \""+elem.getName()+"\" and H.state =\"completed\");\n");

                    sb.append("@Priority(5) context partitionedByPmIDAndCaseID on ProcessEvent(nodeID=\"EE-1\", state=\"completed\") as a\n" +
                            "delete from Execution_History as H\n" +
                            "where H.pmID = a.pmID and H.caseID = a.caseID\n" +
                            "and not exists (select 1 from Execution_History as H where H.pmID = a.pmID and H.caseID = a.caseID and\n" +
                            "H.nodeID = \""+elem.getName()+"\" and H.state =\"completed\");\n");
                }
                else if (elem instanceof TaskImpl)
                {
                    SequenceFlow s = elem.getIncoming().stream().collect(Collectors.toList()).get(0);
                    String condition = s.getName() == null ||  s.getName().length()==0? "true": s.getName();

                    sb.append("// Activity "+elem.getName()+"\n" +
                            "// Template to handle activity nodes that have a single predecessor\n" +
                            "@Name('Activity-"+elem.getName()+"-Start') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID, cycleNum, state, payLoad, Time_stamp)\n" +
                            "select pred.pmID, pred.caseID, \""+elem.getName()+"\", pred.cycleNum,\n" +
                            "case when pred.state=\"completed\" and  evaluate(CV.variables, \""+condition+"\") = true then \"started\" else \"skipped\" end,\n" +
                            "CV.variables, pred.timestamp\n" +
                            "From ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID\n" +
                            "where pred.state in (\"completed\", \"skipped\") and pred.nodeID in "+ inList +";\n");


                    if (((TaskImpl) elem).getIoSpecification() != null &&
                            ((TaskImpl) elem).getIoSpecification().getDataOutputs() != null &&
                            ((TaskImpl) elem).getIoSpecification().getDataOutputs().size() > 0)
                    {

                        sb.append("//Update case variable on the completion of activity " + elem.getName() + "\n" +
                                "context partitionedByPmIDAndCaseID \n" +
                                "on ProcessEvent(nodeID=\"" + elem.getName() + "\", state=\"completed\") as a\n" +
                                "update Case_Variables as CV");
                        String updates="";
                        for (DataOutput doa :((TaskImpl) elem).getIoSpecification().getDataOutputs())
                        {

                            updates +="set variables('"+doa.getName()+"') = a.payLoad('cond1'),\n";
                        }
                        sb.append(updates.substring(0,updates.length()-2)).append("\n");
                        sb.append("where CV.pmID = a.pmID and CV.caseID = a.caseID;\n");
                    }
                }
                else if (elem instanceof ParallelGatewayImpl)
                {
                    if (((ParallelGatewayImpl) elem).getGatewayDirection() == GatewayDirection.Converging)
                    {
                        sb.append("// AND-join\n" +
                                "@Priority(5)\n" +
                                "@Name('AND-Join') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID, cycleNum, state, payLoad, timestamp)\n" +
                                "select pred.pmID, pred.caseID, \""+elem.getName()+"\", pred.cycleNum, case pred.state when \"completed\" then \"completed\" else \"skipped\" end,pred.payLoad, pred.timestamp\n" +
                                "from ProcessEvent as pred\n" +
                                "where pred.state in (\"completed\", \"skipped\") and pred.nodeID in "+inList+"\n" +
                                "and (select count (*) from Execution_History as H where H.nodeID in "+inList+" and H.cycleNum = pred.cycleNum\n" +
                                "and H.state = pred.state and H.pmID = pred.pmID) = "+(predecessors.size()-1)+";\n");
                    }
                    else
                    {
                        SequenceFlow s = elem.getIncoming().stream().collect(Collectors.toList()).get(0);
                        String condition = s.getName() == null || s.getName().length()==0? "true": s.getName();

                        sb.append("// AND Split "+elem.getName()+"\n" +
                                "// Template to handle activity nodes that have a single predecessor\n" +
                                "@Name('AND-Split-"+elem.getName()+"') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID, cycleNum, state, payLoad, Time_stamp)\n" +
                                "select pred.pmID, pred.caseID, \""+elem.getName()+"\", pred.cycleNum,\n" +
                                "case when pred.state=\"completed\" and  evaluate(CV.variables, \""+condition+"\") = true then \"completed\" else \"skipped\" end,\n" +
                                "CV.variables, pred.timestamp\n" +
                                "from ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID\n" +
                                "where pred.state in (\"completed\", \"skipped\") and pred.nodeID in "+inList+";\n");
                    }
                }
                else if (elem instanceof InclusiveGatewayImpl)
                {
                    if (((InclusiveGatewayImpl) elem).getGatewayDirection() == GatewayDirection.Converging)
                    {
                        sb.append("// OR-join\n" +
                                "@Name('OR-Join') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID, cycleNum, state, payLoad, timestamp)\n" +
                                "select pred.pmID, pred.caseID, \"OJ-1\", pred.cycleNum, case\n" +
                                "when (pred.state=\"completed\" or (select count(1) from Execution_History as H where H.nodeID in "+inList+ " and H.cycleNum = pred.cycleNum and H.state=\"completed\") >=1) then \"completed\" else \"skipped\" end,\n" +
                                "pred.payLoad, pred.timestamp\n" +
                                "from ProcessEvent as pred\n" +
                                "where pred.state in (\"completed\", \"skipped\") and pred.nodeID in "+inList+"\n" +
                                "and (select count(1) from Execution_History as H where H.nodeID in "+inList+" and H.cycleNum = pred.cycleNum and H.pmID = pred.pmID and H.caseID= pred.caseID\n" +
                                "and H.state in (\"completed\", \"skipped\")) = "+(predecessors.size()-1)+";\n");
                    }
                    else
                    {
                        SequenceFlow s = elem.getIncoming().stream().collect(Collectors.toList()).get(0);
                        String condition = s.getName() == null || s.getName().length()==0? "true": s.getName();

                        sb.append("// OR Split "+elem.getName()+"\n" +
                                "// Template to handle activity nodes that have a single predecessor\n" +
                                "@Name('OR-Split-"+elem.getName()+"') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID, cycleNum, state, payLoad, Time_stamp)\n" +
                                "select pred.pmID, pred.caseID, \""+elem.getName()+"\", pred.cycleNum,\n" +
                                "case when pred.state=\"completed\" and  evaluate(CV.variables, \""+condition+"\") = true then \"completed\" else \"skipped\" end,\n" +
                                "CV.variables, pred.timestamp\n" +
                                "from ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID\n" +
                                "where pred.state in (\"completed\", \"skipped\") and pred.nodeID in "+inList+";\n");
                    }
                }
                else if (elem instanceof ExclusiveGatewayImpl)
                {
                    if (((ExclusiveGatewayImpl) elem).getGatewayDirection() == GatewayDirection.Converging)
                    {
                        //We need to check if there is a loop
                    }
                    else
                    {
                        SequenceFlow s = elem.getIncoming().stream().collect(Collectors.toList()).get(0);
                        String condition = s.getName() == null || s.getName().length()==0? "true": s.getName();

                        sb.append("// XOR Split "+elem.getName()+"\n" +
                                "// Template to handle activity nodes that have a single predecessor\n" +
                                "@Name('XOR-Split-"+elem.getName()+"') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID, cycleNum, state, payLoad, Time_stamp)\n" +
                                "select pred.pmID, pred.caseID, \""+elem.getName()+"\", pred.cycleNum,\n" +
                                "case when pred.state=\"completed\" and  evaluate(CV.variables, \""+condition+"\") = true then \"completed\" else \"skipped\" end,\n" +
                                "CV.variables, pred.timestamp\n" +
                                "from ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID\n" +
                                "where pred.state in (\"completed\", \"skipped\") and pred.nodeID in "+inList+";\n");
                    }
                }
            }

        }


        return sb.toString();
    }
    public List<String> getNodeIDs()
    {


       return instance.getModelElementsByType(FlowNode.class).stream().map( elem -> {
           return elem.getName() + ", previous:"+elem.getPreviousNodes().list().stream().map(e -> e.getName()).collect(Collectors.toList()).toString();
        }).collect(Collectors.toList());


    }


}
