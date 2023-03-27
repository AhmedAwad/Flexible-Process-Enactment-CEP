package ee.ut.dsg.process.encatment.cep;
import com.bpmnq.*;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.GatewayDirection;
import org.camunda.bpm.model.bpmn.impl.instance.*;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;


import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


public class RulesGenerator {

    private final BpmnModelInstance instance;
    private static final String looplessEntryNode = "-1";
    private static final String loopEntryNode = "-XOR-Split-1";
    private static final String xorJoin = "-XOR-Join-1";

    public RulesGenerator(File file) {
        instance = Bpmn.readModelFromFile(file);
    }

    private QueryGraph buildLoopCheckQueryGraph(String xorJoinID, String xorJoinName) {
        QueryGraph result = new QueryGraph();

        GraphObject xorSplit = new GraphObject(loopEntryNode, "XOR-Split", GraphObject.GraphObjectType.GATEWAY, GraphObject.GateWayType.XOR_SPLIT.asType2String());
        GraphObject xorJoin = new GraphObject(xorJoinID, xorJoinName, GraphObject.GraphObjectType.GATEWAY, GraphObject.GateWayType.XOR_JOIN.asType2String());

        GraphObject generic = new GraphObject(looplessEntryNode, "-1", GraphObject.GraphObjectType.ACTIVITY, GraphObject.ActivityType.GENERIC_SHAPE.asType2String());
        result.add(xorSplit);
        result.add(xorJoin);
        result.add(generic);

        result.addEdge(generic, xorJoin);
        Path p = new Path(xorJoin, xorSplit, generic.getID());
        Path p2 = new Path(xorSplit, xorJoin, generic.getID());
        result.add(p);
        result.add(p2);
        result.addNegativePath(xorSplit, generic);
        return result;
    }

    public ProcessGraph buildBPMNQProcessGraph() {
        ProcessGraph result = new ProcessGraph();
        for (FlowNode elem : instance.getModelElementsByType(FlowNode.class)) {
            GraphObject obj = null;
            if (elem instanceof org.camunda.bpm.model.bpmn.impl.instance.StartEventImpl) {
                obj = new GraphObject(elem.getId(), elem.getName(), GraphObject.GraphObjectType.EVENT, GraphObject.EventType.START.asType2String());

            } else if (elem instanceof org.camunda.bpm.model.bpmn.impl.instance.EndEventImpl) {
                obj = new GraphObject(elem.getId(), elem.getName(), GraphObject.GraphObjectType.EVENT, GraphObject.EventType.END.asType2String());
            } else if (elem instanceof org.camunda.bpm.model.bpmn.impl.instance.TaskImpl) {
                obj = new GraphObject(elem.getId(), elem.getName(), GraphObject.GraphObjectType.ACTIVITY, GraphObject.ActivityType.TASK.asType2String());
            } else if (elem instanceof ParallelGatewayImpl) {
                obj = new GraphObject(elem.getId(), elem.getName(), GraphObject.GraphObjectType.GATEWAY,
                        ((ParallelGatewayImpl) elem).getGatewayDirection() == GatewayDirection.Converging ? GraphObject.GateWayType.AND_JOIN.asType2String() : GraphObject.GateWayType.AND_SPLIT.asType2String());
            } else if (elem instanceof ExclusiveGatewayImpl) {
                obj = new GraphObject(elem.getId(), elem.getName(), GraphObject.GraphObjectType.GATEWAY,
                        ((ExclusiveGatewayImpl) elem).getGatewayDirection() == GatewayDirection.Converging ? GraphObject.GateWayType.XOR_JOIN.asType2String() : GraphObject.GateWayType.XOR_SPLIT.asType2String());
            } else if (elem instanceof InclusiveGatewayImpl) {
                obj = new GraphObject(elem.getId(), elem.getName(), GraphObject.GraphObjectType.GATEWAY,
                        ((InclusiveGatewayImpl) elem).getGatewayDirection() == GatewayDirection.Converging ? GraphObject.GateWayType.OR_JOIN.asType2String() : GraphObject.GateWayType.OR_SPLIT.asType2String());
            }
            if (obj != null)
                result.add(obj);
        }
        for (SequenceFlow flow : instance.getModelElementsByType(SequenceFlow.class)) {
            GraphObject source = result.getNodeByID(flow.getSource().getId());
            GraphObject target = result.getNodeByID(flow.getTarget().getId());
            if (source != null && target != null) {
                result.addEdge(source, target);
            }
        }

        return result;
    }

    public String generateEPLModule() {
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
                "        if (cond == \"Cond1\")\n" +
                "        {\n" +
                "            return caseVariables.get('Cond1');\n" +
                "        }\n" +
                "        if (cond == \"Cond2\")\n" +
                "        {\n" +
                "            return caseVariables.get('Cond2');\n" +
                "        }\n" +
                "        if (cond == \"Cond3\")\n" +
                "        {\n" +
                "            return caseVariables.get('Cond3');\n" +
                "        }\n" +
                "        if (cond == \"Cond4\")\n" +
                "        {\n" +
                "            return caseVariables.get('Cond4');\n" +
                "        }\n" +
                "        return false;\n" +
                "    }\n" +
                "];\n");

        //Add the named window to track execution history per case
        sb.append("//History (named window)\n" +
                "context partitionedByPmIDAndCaseID create window Execution_History.win:keepall as  ProcessEvent;\n");

        //Add events to the named window.
        sb.append("@Priority(10)\n" +
                "@Name(\"Add-to-named-Window\") context partitionedByPmIDAndCaseID\n" +
                "insert into Execution_History(pmID, caseID, nodeID, cycleNum, state, payLoad, timestamp)\n" +
                "select event.pmID, event.caseID, event.nodeID, event.cycleNum, event.state, event.payLoad, event.timestamp from ProcessEvent as event;\n");

        for (FlowNode elem : instance.getModelElementsByType(FlowNode.class)) {
            List<String> predecessors = elem.getPreviousNodes().list().stream().map(e -> "\"" + e.getName() + "\"").collect(Collectors.toList());
            String inList = predecessors.toString().replace("[", "(").replace("]", ")");
            if (elem instanceof org.camunda.bpm.model.bpmn.impl.instance.StartEventImpl) {
                sb.append("// Start event -- this shall be injected from outside\n" +
                        "@Name('Start-Event-"+elem.getName()+"') context partitionedByPmIDAndCaseID " +
                        "insert into ProcessEvent(pmID, caseID, nodeID, cycleNum, state, payLoad, timestamp)\n" +
                        "select pred.pmID, Coalesce((select max(caseID)+1 from Execution_History where pmID = pred.pmID),1), \"" + elem.getName() + "\",0,\"completed\", pred.payLoad, pred.timestamp\n" +
                        "from ProcessEvent(nodeID=\"" + elem.getName() + "\", state=\"started\") as pred;\n" +
                        "//Inititate case variables as a response to the start event\n" +
                        "@Name('Insert-Case-Variables') context partitionedByPmIDAndCaseID\n" +
                        "insert into Case_Variables (pmID, caseID, variables )\n" +
                        "select st.pmID, st.caseID, st.payLoad from ProcessEvent(nodeID=\"" + elem.getName() + "\", state=\"completed\") as st;\n");
            } else if (elem instanceof EndEventImpl) {

                //Generate a skipped or completed event based on the predecessor
                sb.append("// End event\n" +
                        "@Priority(200) @Name('End-Event') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID, cycleNum, state, payLoad, timestamp)\n" +
                        "select pred.pmID, pred.caseID, \"" + elem.getName() + "\", pred.cycleNum,\n" +
                        "case when pred.state=\"completed\" then \"completed\" else \"skipped\" end,\n" +
                        "CV.variables, pred.timestamp\n" +
                        "From ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID\n" +
                        "where pred.state in (\"completed\", \"skipped\") and pred.nodeID in " + inList + ";\n");
//                        "and not exists (select 1 from Execution_History as H where H.pmID = pred.pmID and H.caseID = pred.caseID and\n" +
//                        "H.nodeID = \"" + elem.getName() + "\" and H.state =\"completed\");\n");

                sb.append("@Priority(5) context partitionedByPmIDAndCaseID on ProcessEvent(nodeID=\""+elem.getName()+"\", state=\"completed\") as a\n" +
                        "delete from Execution_History as H\n" +
                        "where H.pmID = a.pmID and H.caseID = a.caseID\n" +
                        "and not exists (select 1 from Execution_History as H where H.pmID = a.pmID and H.caseID = a.caseID and\n" +
                        "H.nodeID = \"" + elem.getName() + "\" and H.state =\"completed\");\n");
            } else if (elem instanceof TaskImpl) {
                SequenceFlow s = new ArrayList<>(elem.getIncoming()).get(0);
                String condition = s.getName() == null || s.getName().length() == 0 ? "true" : s.getName();

                sb.append("// Activity " + elem.getName() + "\n" +
                        "// Template to handle activity nodes that have a single predecessor\n" +
                        "@Name('Activity-" + elem.getName() + "-Start') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID, cycleNum, state, payLoad, Time_stamp)\n" +
                        "select pred.pmID, pred.caseID, \"" + elem.getName() + "\", pred.cycleNum,\n" +
                        "case when pred.state=\"completed\" and  evaluate(CV.variables, \"" + condition + "\") = true then \"started\" else \"skipped\" end,\n" +
                        "CV.variables, pred.timestamp\n" +
                        "From ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID\n" +
                        "where pred.state in (\"completed\", \"skipped\") and pred.nodeID in " + inList + ";\n");


                if (((TaskImpl) elem).getIoSpecification() != null &&
                        ((TaskImpl) elem).getIoSpecification().getDataOutputs() != null &&
                        ((TaskImpl) elem).getIoSpecification().getDataOutputs().size() > 0) {

                    sb.append("//Update case variable on the completion of activity " + elem.getName() + "\n" +
                            "context partitionedByPmIDAndCaseID \n" +
                            "on ProcessEvent(nodeID=\"" + elem.getName() + "\", state=\"completed\") as a\n" +
                            "update Case_Variables as CV ");
                    String updates = "";
                    for (DataOutput doa : ((TaskImpl) elem).getIoSpecification().getDataOutputs()) {

                        updates += "set variables('" + doa.getName() + "') = a.payLoad('" + doa.getName() + "'),\n";
                    }
                    sb.append(updates.substring(0, updates.length() - 2)).append("\n");
                    sb.append("where CV.pmID = a.pmID and CV.caseID = a.caseID;\n");
                }
            } else if (elem instanceof ParallelGatewayImpl) {
                if (((ParallelGatewayImpl) elem).getGatewayDirection() == GatewayDirection.Converging) {
                    sb.append("// AND-join\n" +
                            "@Priority(5)\n" +
                            "@Name('AND-Join') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID, cycleNum, state, payLoad, timestamp)\n" +
                            "select pred.pmID, pred.caseID, \"" + elem.getName() + "\", pred.cycleNum, case pred.state when \"completed\" then \"completed\" else \"skipped\" end,pred.payLoad, pred.timestamp\n" +
                            "from ProcessEvent as pred\n" +
                            "where pred.state in (\"completed\", \"skipped\") and pred.nodeID in " + inList + "\n" +
                            "and (select count (*) from Execution_History as H where H.nodeID in " + inList + " and H.cycleNum = pred.cycleNum\n" +
                            "and H.state = pred.state and H.pmID = pred.pmID) = " + (predecessors.size() - 1) + ";\n");
                } else {
                    SequenceFlow s = elem.getIncoming().stream().collect(Collectors.toList()).get(0);
                    String condition = s.getName() == null || s.getName().length() == 0 ? "true" : s.getName();

                    sb.append("// AND Split " + elem.getName() + "\n" +
                            "// Template to handle activity nodes that have a single predecessor\n" +
                            "@Name('AND-Split-" + elem.getName() + "') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID, cycleNum, state, payLoad, Time_stamp)\n" +
                            "select pred.pmID, pred.caseID, \"" + elem.getName() + "\", pred.cycleNum,\n" +
                            "case when pred.state=\"completed\" and  evaluate(CV.variables, \"" + condition + "\") = true then \"completed\" else \"skipped\" end,\n" +
                            "CV.variables, pred.timestamp\n" +
                            "from ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID\n" +
                            "where pred.state in (\"completed\", \"skipped\") and pred.nodeID in " + inList + ";\n");
                }
            } else if (elem instanceof InclusiveGatewayImpl) {
                if (((InclusiveGatewayImpl) elem).getGatewayDirection() == GatewayDirection.Converging) {
                    sb.append("// OR-join\n" +
                            "@Name('OR-Join') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID, cycleNum, state, payLoad, timestamp)\n" +
                            "select pred.pmID, pred.caseID, \"OJ-1\", pred.cycleNum, case\n" +
                            "when (pred.state=\"completed\" or (select count(1) from Execution_History as H where H.nodeID in " + inList + " and H.cycleNum = pred.cycleNum and H.state=\"completed\") >=1) then \"completed\" else \"skipped\" end,\n" +
                            "pred.payLoad, pred.timestamp\n" +
                            "from ProcessEvent as pred\n" +
                            "where pred.state in (\"completed\", \"skipped\") and pred.nodeID in " + inList + "\n" +
                            "and (select count(1) from Execution_History as H where H.nodeID in " + inList + " and H.cycleNum = pred.cycleNum and H.pmID = pred.pmID and H.caseID= pred.caseID\n" +
                            "and H.state in (\"completed\", \"skipped\")) = " + (predecessors.size() - 1) + ";\n");
                } else {
                    SequenceFlow s = elem.getIncoming().stream().collect(Collectors.toList()).get(0);
                    String condition = s.getName() == null || s.getName().length() == 0 ? "true" : s.getName();

                    sb.append("// OR Split " + elem.getName() + "\n" +
                            "// Template to handle activity nodes that have a single predecessor\n" +
                            "@Name('OR-Split-" + elem.getName() + "') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID, cycleNum, state, payLoad, Time_stamp)\n" +
                            "select pred.pmID, pred.caseID, \"" + elem.getName() + "\", pred.cycleNum,\n" +
                            "case when pred.state=\"completed\" and  evaluate(CV.variables, \"" + condition + "\") = true then \"completed\" else \"skipped\" end,\n" +
                            "CV.variables, pred.timestamp\n" +
                            "from ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID\n" +
                            "where pred.state in (\"completed\", \"skipped\") and pred.nodeID in " + inList + ";\n");
                }
            } else if (elem instanceof ExclusiveGatewayImpl) {
                if (((ExclusiveGatewayImpl) elem).getGatewayDirection() == GatewayDirection.Converging) {
                    //We need to check if there is a loop

                    QueryGraph qry = buildLoopCheckQueryGraph(elem.getId(), elem.getName());
                    ProcessGraph graph = buildBPMNQProcessGraph();
                    MemoryQueryProcessor qp = new MemoryQueryProcessor(null);
                    qp.stopAtFirstMatch = false;
                    ProcessGraph result = qp.runQueryAgainstModel(qry, graph);
                    if (result.nodes.size() > 0) {
                        // there is a loop structure, and we have to have separate rules
                        GraphObject match = result.getNodeByID(elem.getId());
                        if (match != null) {
                            StringBuilder looplessPredecessors = new StringBuilder();
                            List<String> loopyPredecessors = new ArrayList<>();
                            looplessPredecessors.append("(");
                            for (GraphObject node : result.nodes) {
                                if (node.getBoundQueryObjectID().equals(looplessEntryNode)) {
                                    looplessPredecessors.append("\"").append(node.getName()).append("\"").append(",");
                                }
                                else if (node.getBoundQueryObjectID().equals(loopEntryNode))
                                {
                                    loopyPredecessors.add(node.getID());
                                }
                            }
                            looplessPredecessors.replace(looplessPredecessors.length() - 1, looplessPredecessors.length(), "").append(")");
                            sb.append("// XOR-join, when one of the inputs is forming a loop\n" +
                                    "//The loopless entry point\n" +
                                    "@Name('XOR-Join-" + elem.getName() + "') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID, cycleNum, state, payLoad, timestamp)\n" +
                                    "select pred.pmID, pred.caseID, \"" + elem.getName() + "\", pred.cycleNum, case pred.state when \"completed\" then \"completed\" else \"skipped\" end,\n" +
                                    " CV.variables, pred.timestamp\n" +
                                    "from ProcessEvent (state in (\"completed\",\"skipped\") , nodeID in " + looplessPredecessors + ") as pred join Case_Variables as CV\n" +
                                    "on pred.pmID = CV.pmID and pred.caseID = CV.caseID;\n");

                            //Now, let's handle the loop entry point
                            //We can find the difference between inList and looplessPredecessors in this section of the code
                            for (String predID : loopyPredecessors)
                            {
                                for (SequenceFlow s : elem.getIncoming())
                                {
                                    if (s.getSource().getId().equals(predID))
                                    {
                                        String condition = s.getName() == null || s.getName().length() == 0 ? "true" : s.getName();
                                        sb.append("@Name('XOR-Join-loop-")
                                                .append(elem.getName())
                                                .append("') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID, cycleNum, state, payLoad, timestamp)\n")
                                                .append("select pred.pmID, pred.caseID, \"+elem.getName()+\", pred.cycleNum+1, pred.state,\n")
                                                .append(" CV.variables, pred.timestamp\n")
                                                .append("from ProcessEvent (state in (\"completed\") , nodeID = \"")
                                                .append(s.getSource().getName())
                                                .append("\") as pred join Case_Variables as CV\n")
                                                .append("on pred.pmID = CV.pmID and pred.caseID = CV.caseID\n")
                                                .append("where evaluate(CV.variables, \"cond3\")=true;\n");
                                    }
                                }
                            }




                        }
                    } else {
                        //This is a normal Exclusive choice block
                        sb.append("// XOR-join, when one of the inputs is forming a loop\n" +
                                "//The loopless entry point\n" +
                                "@Name('XOR-Join-" + elem.getName() + "') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID, cycleNum, state, payLoad, timestamp)\n" +
                                "select pred.pmID, pred.caseID, \"" + elem.getName() + "\", pred.cycleNum, case pred.state when \"completed\" then \"completed\" else \"skipped\" end,\n" +
                                " CV.variables, pred.timestamp\n" +
                                "from ProcessEvent (state in (\"completed\",\"skipped\") , nodeID in\"" + inList + "\") as pred join Case_Variables as CV\n" +
                                "on pred.pmID = CV.pmID and pred.caseID = CV.caseID;\n");
                    }
                } else {
                    SequenceFlow s = elem.getIncoming().stream().collect(Collectors.toList()).get(0);
                    String condition = s.getName() == null || s.getName().length() == 0 ? "true" : s.getName();

                    sb.append("// XOR Split " + elem.getName() + "\n" +
                            "// Template to handle activity nodes that have a single predecessor\n" +
                            "@Name('XOR-Split-" + elem.getName() + "') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID, cycleNum, state, payLoad, Time_stamp)\n" +
                            "select pred.pmID, pred.caseID, \"" + elem.getName() + "\", pred.cycleNum,\n" +
                            "case when pred.state=\"completed\" and  evaluate(CV.variables, \"" + condition + "\") = true then \"completed\" else \"skipped\" end,\n" +
                            "CV.variables, pred.timestamp\n" +
                            "from ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID\n" +
                            "where pred.state in (\"completed\", \"skipped\") and pred.nodeID in " + inList + ";\n");
                }
            }
        }


        return sb.toString();
    }

    public List<String> getNodeIDs() {


        return instance.getModelElementsByType(FlowNode.class).stream().map(elem -> elem.getName() + ", previous:" + elem.getPreviousNodes().list().stream().map(FlowElement::getName).collect(Collectors.toList())).collect(Collectors.toList());


    }

    private String difference(String inList1, String inList2) {
        //We assume that these are two comma separated list with some parenthesis
        StringBuilder result = new StringBuilder();
        for (String s1 : inList1.split(",")) {
            s1 = s1.replace("(", "").replace(")", "").trim();
            if (!inList2.contains(s1))
                result.append(s1).append(",");
        }
        result.insert(0, "(");
        result.replace(result.length() - 1, result.length(), "");
        result.append(")");
        return result.toString();
    }

}
