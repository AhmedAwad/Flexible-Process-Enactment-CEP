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
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class BPMNRulesGenerator extends RuleGenerator {



    private final BpmnModelInstance instance;
    private static final String looplessEntryNode = "-1";
    private static final String loopEntryNode = "-XOR-Split";
//    private static final String xorJoin = "-XOR-Join-1";

    private ProcessGraph process;
    private final List<String> loopEntryNodes;
    private final List<String> conditions;

    public BPMNRulesGenerator(File file) {
        instance = Bpmn.readModelFromFile(file);
        loopEntryNodes = new ArrayList<>();
        conditions = new ArrayList<>();
        preprocess();
    }

    private void preprocess()
    {
        for (FlowNode elem : instance.getModelElementsByType(FlowNode.class)) {
            if (elem instanceof ExclusiveGatewayImpl) {
                if (((ExclusiveGatewayImpl) elem).getGatewayDirection() == GatewayDirection.Diverging)
                {
                    QueryGraph qry = buildLoopCheckQueryGraphXorSplit(elem.getId(), elem.getName());
                    if (process == null)
                        process = buildBPMNQProcessGraph();
                    MemoryQueryProcessor qp = new MemoryQueryProcessor(null);
                    qp.stopAtFirstMatch = false;

                    ProcessGraph result = qp.runQueryAgainstModel(qry, process);
                    if (result.nodes.size()> 0)
                    {
                        List<GraphObject> succ = new ArrayList<>();
                        for(GraphObject obj: result.nodes)
                        {
                           if( obj.getID().equals(elem.getId()))
                           {
                               succ = result.getSuccessorsFromGraph(obj);
                               break;

                           }
                        }
                        List<String> succNames=  succ.stream().map (GraphObject::getName).collect(Collectors.toList());
                        for(GraphObject obj: result.nodes)
                        {
                            if (obj.getBoundQueryObjectID().equals("-1"))
                                succNames.remove(obj.getName());
                        }
                        loopEntryNodes.addAll(succNames);
                    }
                }

            }
        }
        for (SequenceFlow elem : instance.getModelElementsByType(SequenceFlow.class))
        {
            if (elem.getName() != null && elem.getName().length() > 0)
                conditions.add(elem.getName().trim());
        }
    }

    private static QueryGraph buildUnstructuredLoopORJoinCheck(String orJoinID, String orJoinName)
    {
        QueryGraph result = new QueryGraph();
        GraphObject orJoin = new GraphObject(orJoinID, orJoinName, GraphObject.GraphObjectType.GATEWAY, GraphObject.GateWayType.OR_JOIN.asType2String());

        GraphObject generic = new GraphObject("-1", "-1", GraphObject.GraphObjectType.ACTIVITY, GraphObject.ActivityType.GENERIC_SHAPE.asType2String());
        GraphObject generic2 = new GraphObject("-2", "-2", GraphObject.GraphObjectType.ACTIVITY, GraphObject.ActivityType.GENERIC_SHAPE.asType2String());

        result.add(orJoin);
        result.add(generic);
        result.add(generic2);

        result.addEdge(generic2, orJoin);

        Path p = new Path(generic, generic2, orJoin.getID());
        Path p2 = new Path(orJoin, generic2, generic.getID());

        result.add(p);
        result.add(p2);


        return result;
    }

    private static QueryGraph buildQueryToCheckCyclicReachabilityOfORJoinInput(String inputNodeID, String inputNodeName, GraphObject.GraphObjectType type1, String type2, String orJoinID, String orJoinName)
    {
        QueryGraph query = new QueryGraph();
        GraphObject orJoin = new GraphObject(orJoinID,orJoinName, GraphObject.GraphObjectType.GATEWAY, GraphObject.GateWayType.OR_JOIN.asType2String());
        GraphObject input = new GraphObject(inputNodeID,inputNodeName, type1, type2);

        query.add(orJoin);
        query.add(input);
        query.addEdge(input, orJoin);

        Path p = new Path(orJoin,input);


        query.add(p);

        return query;

    }
    private static QueryGraph buildQueryToCheckAcyclicReachabilityOfORJoinInput(String inputNodeID, String inputNodeName, GraphObject.GraphObjectType type1, String type2, String orJoinID, String orJoinName)
    {
        QueryGraph query = new QueryGraph();
        GraphObject orJoin = new GraphObject(orJoinID,orJoinName, GraphObject.GraphObjectType.GATEWAY, GraphObject.GateWayType.OR_JOIN.asType2String());
        GraphObject startEvent = new GraphObject("-1","$#", GraphObject.GraphObjectType.EVENT, GraphObject.EventType.START.asType2String());
        GraphObject input = new GraphObject(inputNodeID,inputNodeName, type1, type2);

        query.add(orJoin);
        query.add(startEvent);
        query.add(input);
        query.addEdge(input, orJoin);

        Path p = new Path(startEvent, orJoin);
        p.setPathEvaluaiton(Path.PathEvaluation.ACYCLIC);
        Path p2 = new Path(startEvent, input, orJoin.toString());
        p2.setPathEvaluaiton(Path.PathEvaluation.ACYCLIC);

        query.add(p);
        query.add(p2);
        return query;

    }
    private static QueryGraph buildUnstructuredORBlocK(String orJoinID, String orJoinName)
    {
        QueryGraph query = new QueryGraph();

        GraphObject orJoin = new GraphObject(orJoinID,orJoinName, GraphObject.GraphObjectType.GATEWAY, GraphObject.GateWayType.OR_JOIN.asType2String());
        GraphObject split = new GraphObject("-1","?split", GraphObject.GraphObjectType.GATEWAY, GraphObject.GateWayType.GENERIC_SPLIT.asType2String());

        GraphObject in1 = new GraphObject("-2", "?in1", GraphObject.GraphObjectType.ACTIVITY, GraphObject.ActivityType.GENERIC_SHAPE.asType2String());
//        GraphObject in2 = new GraphObject("-3", "?in2", GraphObject.GraphObjectType.ACTIVITY, GraphObject.ActivityType.GENERIC_SHAPE.asType2String());
        GraphObject out = new GraphObject("-4", "?out", GraphObject.GraphObjectType.GATEWAY, GraphObject.GateWayType.GENERIC_SPLIT.asType2String());

        query.add(orJoin);
        query.add(split);
        query.add(in1);
//        query.add(in2);
        query.add(out);

        query.addEdge(in1, orJoin);
//        query.addEdge(in2, orJoin);
        Path p1 = new Path(split, in1,"?out");
        p1.setPathEvaluaiton(Path.PathEvaluation.SHORTEST);
        Path p2 = new Path(split, orJoin, "?out");
        p2.setPathEvaluaiton(Path.PathEvaluation.SHORTEST);
        Path p3 = new Path(out, in1, "?split");
        p3.setPathEvaluaiton(Path.PathEvaluation.SHORTEST);

        query.add(p1);
        query.add(p2);
        query.add(p3);
        return query;
    }
    private static QueryGraph buildLoopCheckQueryGraph(String xorJoinID, String xorJoinName) {
        QueryGraph result = new QueryGraph();

        GraphObject xorSplit = new GraphObject(loopEntryNode, "XOR-Split", GraphObject.GraphObjectType.GATEWAY, GraphObject.GateWayType.XOR_SPLIT.asType2String());
        GraphObject xorJoin = new GraphObject(xorJoinID, xorJoinName, GraphObject.GraphObjectType.GATEWAY, GraphObject.GateWayType.XOR_JOIN.asType2String());

        GraphObject generic = new GraphObject(looplessEntryNode, "-1", GraphObject.GraphObjectType.ACTIVITY, GraphObject.ActivityType.GENERIC_SHAPE.asType2String());
        GraphObject generic2 = new GraphObject("-2", "-2", GraphObject.GraphObjectType.ACTIVITY, GraphObject.ActivityType.GENERIC_SHAPE.asType2String());

        result.add(xorSplit);
        result.add(xorJoin);
        result.add(generic);
        result.add(generic2);

        result.addEdge(generic, xorJoin);
        result.addEdge(xorSplit, generic2);
        Path p = new Path(xorJoin, xorSplit, generic.getID());
        Path p2 = new Path(xorSplit, xorJoin, generic.getID());
        result.add(p);
        result.add(p2);
        result.addNegativePath(xorSplit, generic);
        result.addNegativePath(generic2,xorJoin);
        return result;
    }

    private static QueryGraph buildLoopCheckQueryGraphXorSplit(String xorSplitID, String xorSplitName) {
        QueryGraph result = new QueryGraph();

        GraphObject xorSplit = new GraphObject(xorSplitID, xorSplitName, GraphObject.GraphObjectType.GATEWAY, GraphObject.GateWayType.XOR_SPLIT.asType2String());
        GraphObject xorJoin = new GraphObject(loopEntryNode, "-XOR-Join", GraphObject.GraphObjectType.GATEWAY, GraphObject.GateWayType.XOR_JOIN.asType2String());

        GraphObject generic = new GraphObject(looplessEntryNode, "-1", GraphObject.GraphObjectType.ACTIVITY, GraphObject.ActivityType.GENERIC_SHAPE.asType2String());
        GraphObject generic2 = new GraphObject("-2", "-2", GraphObject.GraphObjectType.ACTIVITY, GraphObject.ActivityType.GENERIC_SHAPE.asType2String());

        result.add(xorSplit);
        result.add(xorJoin);
        result.add(generic);
        result.add(generic2);

        result.addEdge(xorSplit, generic);
        result.addEdge(xorSplit, generic2);
        Path p = new Path(generic2, xorJoin, generic.getID());
        p.setPathEvaluaiton(Path.PathEvaluation.ACYCLIC);
        Path p2 = new Path(xorJoin, xorSplit, generic.getID());
        p2.setPathEvaluaiton(Path.PathEvaluation.ACYCLIC);
        result.add(p);
        result.add(p2);
        result.addNegativePath(generic, xorJoin);
        //result.addNegativePath(generic2,xorJoin);
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

    private String handleLoopEntry(String nodeName)
    {
        if (loopEntryNodes.contains(nodeName))
            return " + 1";
        return " ";
    }
    @Override
    public String generateEPLModule() {
        StringBuilder sb = new StringBuilder();

        //Create the context and the tracking of events
        createContext(sb);


        //Add the case variables table
        createCaseVariables(sb);


        createConditionEvaluator(sb, conditions);

        //Add the named window to track execution history per case
        defineExecutionHistoryWindow(sb);

        createSynchronizationTable(sb);


        for (FlowNode elem : instance.getModelElementsByType(FlowNode.class)) {
            List<String> predecessors = elem.getPreviousNodes().list().stream().map(e -> "\"" + e.getName() + "\"").collect(Collectors.toList());
            String inList = predecessors.toString().replace("[", "(").replace("]", ")");
            //Check if the successor is a synchronization node
            if (elem.getOutgoing().size() > 0 )
            {
                elem.getOutgoing().stream().forEach(new Consumer<SequenceFlow>() {
                    @Override
                    public void accept(SequenceFlow sequenceFlow) {
                        if (sequenceFlow.getTarget() instanceof ParallelGatewayImpl ) {
                            if (((ParallelGatewayImpl) sequenceFlow.getTarget()).getGatewayDirection() == GatewayDirection.Converging){
                                sb.append("// Add to Synchronization events\n" +
                                        "@Name('Synchronization-Event-" + elem.getName() + "') context partitionedByPmIDAndCaseID " +
                                        "insert into Synchronization_Events(pmID, caseID, nodeID, state, timestamp)\n" +
                                        "select pred.pmID, pred.caseID, pred.nodeID, pred.state, pred.timestamp\n" +
                                        "from ProcessEvent(nodeID=\"" + elem.getName() + "\") as pred;\n");
                        }
                        }
                    }
                });
            }
            if (elem instanceof org.camunda.bpm.model.bpmn.impl.instance.StartEventImpl) {
                sb.append("// Start event -- this shall be injected from outside\n" +
                        "@Name('Start-Event-"+elem.getName()+"') context partitionedByPmIDAndCaseID " +
                        "insert into ProcessEvent(pmID, caseID, nodeID, cycleNum, state, payLoad, timestamp)\n" +
                        "select pred.pmID, pred.caseID, \"" + elem.getName() + "\",0," + COMPLETED + ", pred.payLoad, pred.timestamp\n" +
                        "from ProcessEvent(nodeID=\"" + elem.getName() + "\", state=" + STARTED + ") as pred;\n" +
                        "//Inititate case variables as a response to the start event\n" +
                        "@Name('Insert-Case-Variables') context partitionedByPmIDAndCaseID\n" +
                        "insert into Case_Variables (pmID, caseID, variables )\n" +
                        "select st.pmID, st.caseID, st.payLoad from ProcessEvent(nodeID=\"" + elem.getName() + "\", state=" + STARTED + ") as st;\n");
            } else if (elem instanceof EndEventImpl) {

                SequenceFlow s = elem.getIncoming().stream().collect(Collectors.toList()).get(0);
                String condition = s.getName() == null || s.getName().length() == 0 ? "true" : s.getName();

                //Generate a skipped or completed event based on the predecessor
                sb.append("// End event\n" +
                        "@Priority(200) @Name('End-Event') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID, cycleNum, state, payLoad, timestamp)\n" +
                        "select pred.pmID, pred.caseID, \"" + elem.getName() + "\", pred.cycleNum,\n" +
                        "case when pred.state=" + COMPLETED + "  and  evaluate(CV.variables, \"" + condition + "\") = true then " + COMPLETED + " else " + SKIPPED + " end,\n" +
                        "CV.variables, pred.timestamp\n" +
                        "From ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID\n" +
                        "where pred.state in (" + COMPLETED + ", " + SKIPPED + ") and pred.nodeID in " + inList + ";\n");
//                        "and not exists (select 1 from Execution_History as H where H.pmID = pred.pmID and H.caseID = pred.caseID and\n" +
//                        "H.nodeID = \"" + elem.getName() + "\" and H.state =\"completed\");\n");

                sb.append("@Priority(5) context partitionedByPmIDAndCaseID on ProcessEvent(nodeID=\""+elem.getName()+ "\", state=" + COMPLETED + ") as a\n" +
                        "delete from Execution_History as H\n" +
                        "where H.pmID = a.pmID and H.caseID = a.caseID\n" +
                        "and not exists (select 1 from Execution_History as H where H.pmID = a.pmID and H.caseID = a.caseID and\n" +
                        "H.nodeID = \"" + elem.getName() + "\" and H.state =" + COMPLETED + ");\n");
            } else if (elem instanceof TaskImpl) {
                SequenceFlow s = new ArrayList<>(elem.getIncoming()).get(0);
                String condition = s.getName() == null || s.getName().length() == 0 ? "true" : s.getName();

                sb.append("// Activity " + elem.getName() + "\n" +
                        "// Template to handle activity nodes that have a single predecessor\n" +
                        "@Name('Activity-" + elem.getName() + "-Start') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID, cycleNum, state, payLoad, Time_stamp)\n" +
                        "select pred.pmID, pred.caseID, \"" + elem.getName() + "\", pred.cycleNum"+  handleLoopEntry(elem.getName())+ ",\n" +
                        "case when pred.state=" + COMPLETED + " and  evaluate(CV.variables, \"" + condition + "\") = true then " + STARTED + " else " + SKIPPED + " end,\n" +
                        "CV.variables, pred.timestamp\n" +
                        "From ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID\n" +
                        "where pred.state in (" + COMPLETED + ", " + SKIPPED + ") and pred.nodeID in " + inList + ";\n");


                if (((TaskImpl) elem).getIoSpecification() != null &&
                        ((TaskImpl) elem).getIoSpecification().getDataOutputs() != null &&
                        ((TaskImpl) elem).getIoSpecification().getDataOutputs().size() > 0) {

                    sb.append("//Update case variable on the completion of activity " + elem.getName() + "\n" +
                            "context partitionedByPmIDAndCaseID \n" +
                            "on ProcessEvent(nodeID=\"" + elem.getName() + "\", state=" + COMPLETED + ") as a\n" +
                            "update Case_Variables as CV set");
                    String updates = "";
                    for (DataOutput doa : ((TaskImpl) elem).getIoSpecification().getDataOutputs()) {

                        updates += " variables('" + doa.getName() + "') = a.payLoad('" + doa.getName() + "'),\n";
                    }
                    sb.append(updates, 0, updates.length() - 2).append("\n");
                    sb.append("where CV.pmID = a.pmID and CV.caseID = a.caseID;\n");
                }
            } else if (elem instanceof ParallelGatewayImpl) {
                if (((ParallelGatewayImpl) elem).getGatewayDirection() == GatewayDirection.Converging) {
                    for (SequenceFlow in : elem.getIncoming()) { // outer loop
                        FlowNode elem2 = in.getSource();
                        for (SequenceFlow in2 : elem.getIncoming()) {
                            FlowNode elem3 = in2.getSource();
                            if (elem3.getId().equals(elem2.getId())) //we look for different nodes
                                continue;
                            //Started case
                            sb.append("// AND-join\n" +
                                    "@Priority(5)\n" +
                                    "@Name('AND-Join-" + elem2.getName() + "-started') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID, cycleNum, state, payLoad, timestamp)\n" +
                                    "select pred.pmID, pred.caseID, \"" + elem.getName() + "\", pred.cycleNum" + handleLoopEntry(elem.getName()) + ", case pred.state when " + COMPLETED + " then " + COMPLETED + " else " + SKIPPED + " end,pred.payLoad, pred.timestamp\n" +
                                    "from ProcessEvent(nodeID=\"" + elem2.getName() + "\", state =" + COMPLETED + ") as pred\n" +
                                    "where \n" +
                                    "(select count (*) from Synchronization_Events as H where H.nodeID = \"" + elem3.getName() + "\" and H.caseID = pred.caseID\n" +
                                    "and H.state = pred.state and H.pmID = pred.pmID) =1 ;\n");
                            sb.append("// AND-join\n" +
                                    "@Priority(5)\n" +
                                    "@Name('AND-Join-" + elem2.getName() + "-skipped') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID, cycleNum, state, payLoad, timestamp)\n" +
                                    "select pred.pmID, pred.caseID, \"" + elem.getName() + "\", pred.cycleNum" + handleLoopEntry(elem.getName()) + ", case pred.state when " + COMPLETED + " then " + COMPLETED + " else " + SKIPPED + " end,pred.payLoad, pred.timestamp\n" +
                                    "from ProcessEvent(nodeID=\"" + elem2.getName() + "\", state =" + SKIPPED + ") as pred\n" +
                                    "where \n" +
                                    "(select count (*) from Synchronization_Events as H where H.nodeID = \"" + elem3.getName() + "\" and H.caseID = pred.caseID\n" +
                                    "and H.state = pred.state and H.pmID = pred.pmID) =1 ;\n");
                        }
                    }


                } else {
                    SequenceFlow s = elem.getIncoming().stream().collect(Collectors.toList()).get(0);
                    String condition = s.getName() == null || s.getName().length() == 0 ? "true" : s.getName();

                    sb.append("// AND Split " + elem.getName() + "\n" +
                            "// Template to handle activity nodes that have a single predecessor\n" +
                            "@Name('AND-Split-" + elem.getName() + "') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID, cycleNum, state, payLoad, Time_stamp)\n" +
                            "select pred.pmID, pred.caseID, \"" + elem.getName() + "\", pred.cycleNum"+  handleLoopEntry(elem.getName())+ ",\n" +
                            "case when pred.state=" + COMPLETED + " and  evaluate(CV.variables, \"" + condition + "\") = true then " + COMPLETED + " else " + SKIPPED + " end,\n" +
                            "CV.variables, pred.timestamp\n" +
                            "from ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID\n" +
                            "where pred.state in (" + COMPLETED + ", " + SKIPPED + ") and pred.nodeID in " + inList + ";\n");
                }
            } else if (elem instanceof InclusiveGatewayImpl) {
                if (((InclusiveGatewayImpl) elem).getGatewayDirection() == GatewayDirection.Converging) {
                    // We need to check if the OR join is part of an unstructured loop, where some branches are decided and others are from the looping entry
                    handleORJoin(sb, elem, predecessors, inList);

                } else {
                    SequenceFlow s = elem.getIncoming().stream().collect(Collectors.toList()).get(0);
                    String condition = s.getName() == null || s.getName().length() == 0 ? "true" : s.getName();

                    sb.append("// OR Split " + elem.getName() + "\n" +
                            "// Template to handle activity nodes that have a single predecessor\n" +
                            "@Name('OR-Split-" + elem.getName() + "') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID, cycleNum, state, payLoad, Time_stamp)\n" +
                            "select pred.pmID, pred.caseID, \"" + elem.getName() + "\", pred.cycleNum"+  handleLoopEntry(elem.getName())+ ",\n" +
                            "case when pred.state=" + COMPLETED + " and  evaluate(CV.variables, \"" + condition + "\") = true then " + COMPLETED + " else " + SKIPPED + " end,\n" +
                            "CV.variables, pred.timestamp\n" +
                            "from ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID\n" +
                            "where pred.state in (" + COMPLETED + ", " + SKIPPED + ") and pred.nodeID in " + inList + ";\n");
                }
            } else if (elem instanceof ExclusiveGatewayImpl) {
                if (((ExclusiveGatewayImpl) elem).getGatewayDirection() == GatewayDirection.Converging) {
                    //We need to check if there is a loop
                    if (loopEntryNodes.contains(elem.getName()))
                    {
                        StringBuilder looplessPredecessors = new StringBuilder();
                        looplessPredecessors.append("(");
                        for (SequenceFlow flow: elem.getIncoming())
                        {
                            if (! (flow.getSource() instanceof ExclusiveGatewayImpl))
                            {
                                looplessPredecessors.append("\"").append(flow.getSource().getName()).append("\"").append(",");
                            }
                            else
                            {
                                String condition = flow.getName() == null || flow.getName().length() == 0 ? "true" : flow.getName();
                                sb.append("@Name('XOR-Join-loop-")
                                        .append(elem.getName())
                                        .append("-input-")
                                        .append(flow.getSource().getName())
                                        .append("') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID, cycleNum, state, payLoad, timestamp)\n")
                                        .append("select pred.pmID, pred.caseID, \""+elem.getName()+"\", pred.cycleNum+1, pred.state,\n")
                                        .append(" CV.variables, pred.timestamp\n")
                                        .append("from ProcessEvent (state in (" + COMPLETED + "," + SKIPPED+") , nodeID = \"")
                                        .append(flow.getSource().getName())
                                        .append("\") as pred join Case_Variables as CV\n")
                                        .append("on pred.pmID = CV.pmID and pred.caseID = CV.caseID\n")
                                        .append("where evaluate(CV.variables, \""+condition+"\")=true;\n");
                            }
                        }
                        looplessPredecessors.replace(looplessPredecessors.length() - 1, looplessPredecessors.length(), "").append(")");
                        sb.append("// XOR-join, when one of the inputs is forming a loop\n" +
                                "//The loopless entry point\n" +
                                "@Name('XOR-Join-" + elem.getName() + "') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID, cycleNum, state, payLoad, timestamp)\n" +
                                "select pred.pmID, pred.caseID, \"" + elem.getName() + "\", pred.cycleNum, case pred.state when " + COMPLETED + " then " + COMPLETED + " else " + SKIPPED + " end,\n" +
                                " CV.variables, pred.timestamp\n" +
                                "from ProcessEvent (state in (" + COMPLETED + "," + SKIPPED+") , nodeID in " + looplessPredecessors + ") as pred join Case_Variables as CV\n" +
                                "on pred.pmID = CV.pmID and pred.caseID = CV.caseID;\n");

                    }


                    else {
                        //This is a normal Exclusive choice block
                        sb.append("// XOR-join, when one of the inputs is forming a loop\n" +
                                "//The loopless entry point\n" +
                                "@Name('XOR-Join-" + elem.getName() + "') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID, cycleNum, state, payLoad, timestamp)\n" +
                                "select pred.pmID, pred.caseID, \"" + elem.getName() + "\", pred.cycleNum, case pred.state when " + COMPLETED + " then " + COMPLETED + " else " + SKIPPED + " end,\n" +
                                " CV.variables, pred.timestamp\n" +
                                "from ProcessEvent (state in (" + COMPLETED + ","+ SKIPPED+"), nodeID in " + inList + ") as pred join Case_Variables as CV\n" +
                                "on pred.pmID = CV.pmID and pred.caseID = CV.caseID where (");
                        for (SequenceFlow flow: elem.getIncoming())
                        {
                            String condition = flow.getName() == null || flow.getName().length() == 0 ? "true" : flow.getName();
                            sb.append("evaluate(CV.variables, \""+condition+"\")=true or ");
                        }
                        sb.append(" false);\n");
                    }
                } else {

                    SequenceFlow s = elem.getIncoming().stream().collect(Collectors.toList()).get(0);
                    String condition = s.getName() == null || s.getName().length() == 0 ? "true" : s.getName();

                    sb.append("// XOR Split " + elem.getName() + "\n" +
                            "// Template to handle activity nodes that have a single predecessor\n" +
                            "@Name('XOR-Split-" + elem.getName() + "') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID, cycleNum, state, payLoad, Time_stamp)\n" +
                            "select pred.pmID, pred.caseID, \"" + elem.getName() + "\", pred.cycleNum,\n" +
                            "case when pred.state=" + COMPLETED + " and  evaluate(CV.variables, \"" + condition + "\") = true then " + COMPLETED + " else " + SKIPPED + " end,\n" +
                            "CV.variables, pred.timestamp\n" +
                            "from ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID\n" +
                            "where pred.state in (" + COMPLETED + ", " + SKIPPED + ") and pred.nodeID in " + inList + ";\n");
                }
            }
            else if (elem instanceof EventBasedGatewayImpl )
            {

            }
        }


        return sb.toString();
    }

    private static void defineExecutionHistoryWindow(StringBuilder sb) {

        sb.append("//History (named window)\n" +
                "context partitionedByPmIDAndCaseID create window Execution_History.win:keepall as  ProcessEvent;\n");
        //Add events to the named window.
        sb.append("@Priority(10)\n" +
                "@Name(\"Add-to-named-Window\") context partitionedByPmIDAndCaseID\n" +
                "insert into Execution_History(pmID, caseID, nodeID, cycleNum, state, payLoad, timestamp)\n" +
                "select event.pmID, event.caseID, event.nodeID, event.cycleNum, event.state, event.payLoad, event.timestamp from ProcessEvent as event;\n");

    }

    private static void createConditionEvaluator(StringBuilder sb,List<String> conditions) {
        sb.append("context partitionedByPmIDAndCaseID\n" +
                "create expression boolean js:evaluate(caseVariables, cond) [\n" +
                "    evaluate(caseVariables, cond);\n" +
                "    function evaluate(caseVariables, cond){\n" +
                "        if (cond == \"true\")\n" +
                "        {\n" +
                "            return true;\n" +
                "        }\n");
        for (String cond : conditions)
        {
            sb.append(" if (cond == \""+cond+"\")\n" +
                    "                {\n" +
                    "                     return caseVariables.get('"+cond+"');\n"+
                    "                 }\n");
        }
       sb.append("\n" +
                "        return false;\n" +
                "    }\n" +
                "];\n");
    }

    private void createCaseVariables(StringBuilder sb) {
        sb.append("//Create the table that holds case variables\n" +
                "context partitionedByPmIDAndCaseID create table Case_Variables (pmID int primary key, caseID int primary key, variables java.util.Map);\n");

        sb.append("@name('track-case-variables') context partitionedByPmIDAndCaseID select  pmID, caseID, variables from Case_Variables;\n");
    }

    private void createSynchronizationTable(StringBuilder sb)
    {
        sb.append("//Create the table that holds events needed for synchronization nodes, AND/OR joins\n" +
                "context partitionedByPmIDAndCaseID create table Synchronization_Events (pmID int primary key, caseID int primary key, nodeID string, state string," +
                " timestamp long primary key);\n");

        sb.append("@name('track-synchronization-events') context partitionedByPmIDAndCaseID select  pmID, caseID, nodeID, state from Synchronization_Events;\n");
    }

    private void handleORJoin(StringBuilder sb, FlowNode elem, List<String> predecessors, String inList) {
        QueryGraph qry;// = buildUnstructuredLoopORJoinCheck(elem.getId(), elem.getName());
        qry = buildUnstructuredORBlocK(elem.getId(), elem.getName());
        if (process == null)
            process = buildBPMNQProcessGraph();
        MemoryQueryProcessor qp = new MemoryQueryProcessor(null);
        qp.stopAtFirstMatch = true;
        qp.allowGenericShapeToEvaluateToNone=true;
        ProcessGraph result = qp.runQueryAgainstModel(qry, process);
        if (result.nodes.size() > 0)
        {
            // Now lets check the cyclic and non-cyclic input nodes
            List<String> acyclic, cyclic;
            acyclic = new ArrayList<>();
            cyclic = new ArrayList<>();
            GraphObject orJoin = result.getNodeByID(elem.getId());

            List<GraphObject> preds = result.getPredecessorsFromGraph(orJoin);
            if (preds.size()> 0)
            for (GraphObject obj: preds)
            {
                qry = buildQueryToCheckAcyclicReachabilityOfORJoinInput(obj.getID(), obj.getName(),obj.type, obj.type2, elem.getId(), elem.getName());

                ProcessGraph result2 = qp.runQueryAgainstModel(qry,process);
                if (result2.nodes.size()> 0)
                {
                    acyclic.add(obj.getName());
                }
                qry = buildQueryToCheckCyclicReachabilityOfORJoinInput(obj.getID(), obj.getName(),obj.type, obj.type2, elem.getId(), elem.getName());
                if (qp.runQueryAgainstModel(qry,process).nodes.size()> 0)
                {
                    cyclic.add(obj.getName());
                }

            }
            acyclic = acyclic.stream().map(e -> "\"" + e + "\"").collect(Collectors.toList());
            String acyclicList = acyclic.toString().replace("[", "(").replace("]", ")");
            sb.append("// OR-join\n" +
                    "@Name('OR-Join-unstructured-loop-"+ elem.getName()+"-cycle-ZERO') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID, cycleNum, state, payLoad, timestamp)\n" +
                    "select pred.pmID, pred.caseID, \""+ elem.getName()+"\", pred.cycleNum"+  handleLoopEntry(elem.getName())+ ", case\n" +
                    "when (pred.state=" + COMPLETED + " or (select count(1) from Execution_History as H where H.nodeID in " + inList + " and H.cycleNum = pred.cycleNum and H.state=" + COMPLETED + ") >=1) then " + COMPLETED + " else " + SKIPPED + " end,\n" +
                    "pred.payLoad, pred.timestamp\n" +
                    "from ProcessEvent as pred\n" +
                    "where pred.state in (" + COMPLETED + ", " + SKIPPED + ") and pred.cycleNum=0 and pred.nodeID in " + acyclicList + "\n" +
                    "and (select count(1) from Execution_History as H where H.nodeID in " + acyclicList + " and H.cycleNum = pred.cycleNum and H.pmID = pred.pmID and H.caseID= pred.caseID\n" +
                    "and H.state in (" + COMPLETED + ", " + SKIPPED + ")) = "+ (acyclic.size()-1) +";\n");
            cyclic = cyclic.stream().map(e -> "\"" + e + "\"").collect(Collectors.toList());
            String cyclicList = cyclic.toString().replace("[", "(").replace("]", ")");

            sb.append("// OR-join\n" +
                    "@Name('OR-Join-unstructured-loop-"+ elem.getName()+"-cycle-greater-than-ZERO') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID, cycleNum, state, payLoad, timestamp)\n" +
                    "select pred.pmID, pred.caseID, \""+ elem.getName()+"\", pred.cycleNum"+  handleLoopEntry(elem.getName())+ ", case\n" +
                    "when (pred.state=" + COMPLETED + " or (select count(1) from Execution_History as H where H.nodeID in " + cyclicList + " and H.cycleNum = pred.cycleNum and H.state=" + COMPLETED + ") >=1) then " + COMPLETED + " else " + SKIPPED + " end,\n" +
                    "pred.payLoad, pred.timestamp\n" +
                    "from ProcessEvent as pred\n" +
                    "where pred.state in (" + COMPLETED + ", " + SKIPPED + ") and pred.cycleNum>0 and pred.nodeID in " + cyclicList + "\n" +
                    "and (select count(1) from Execution_History as H where H.nodeID in " + cyclicList + " and H.cycleNum = pred.cycleNum and H.pmID = pred.pmID and H.caseID= pred.caseID\n" +
                    "and H.state in (" + COMPLETED + ", " + SKIPPED + ")) = "+ (cyclic.size()-1) +";\n");
        }
        else {
            sb.append("// OR-join\n" +
                    "@Name('OR-Join-" + elem.getName() + "') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID, cycleNum, state, payLoad, timestamp)\n" +
                    "select pred.pmID, pred.caseID,\"" + elem.getName() + "\", pred.cycleNum"+  handleLoopEntry(elem.getName())+ ", case\n" +
                    "when (pred.state=" + COMPLETED + " or (select count(1) from Execution_History as H where H.nodeID in " + inList + " and H.cycleNum = pred.cycleNum and H.state=" + COMPLETED + ") >=1) then " + COMPLETED + " else " + SKIPPED + " end,\n" +
                    "pred.payLoad, pred.timestamp\n" +
                    "from ProcessEvent as pred\n" +
                    "where pred.state in (" + COMPLETED + ", " + SKIPPED + ") and pred.nodeID in " + inList + "\n" +
                    "and (select count(1) from Execution_History as H where H.nodeID in " + inList + " and H.cycleNum = pred.cycleNum and H.pmID = pred.pmID and H.caseID= pred.caseID\n" +
                    "and H.state in (" + COMPLETED + ", " + SKIPPED + ")) = " + (predecessors.size() - 1) + ";\n");
        }
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
