package ee.ut.dsg.process.encatment.cep;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.Scanner;

import com.bpmnq.ProcessGraph;
import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventBean;
import com.espertech.esper.common.client.EventSender;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.fireandforget.EPFireAndForgetQueryResult;
import com.espertech.esper.common.client.module.Module;
import com.espertech.esper.common.client.module.ParseException;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.EPCompiler;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.runtime.client.*;
import ee.ut.dsg.process.encatment.cep.events.ProcessEvent;
import ee.ut.dsg.process.encatment.cep.transition.TransitionGraph;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;


public class Runner {

    public static void main(String[] args) {

//        generateBPMNRulesToFile("C:\\Work\\DSG\\Flexible-Process-Enactment-CEP\\src\\etc\\examples\\Manufacturing Process from 2023 paper.bpmn");
        enactEPL("C:\\Work\\DSG\\Flexible-Process-Enactment-CEP\\src\\etc\\examples\\Manufacturing Process from 2023 paper.epl");
//        obtainProcessGraph();
//        System.exit(0);
//        generateRules();
//        System.exit(0);
//        enact("C:\\Work\\DSG\\Flexible-Process-Enactment-CEP\\src\\etc\\examples\\IrreducibleLoop.bpmn");
//        enactBPMN("C:\\Work\\DSG\\Flexible-Process-Enactment-CEP\\src\\etc\\examples\\Process222V7.bpmn");
//        enact("C:\\Work\\DSG\\Flexible-Process-Enactment-CEP\\src\\etc\\examples\\ProcessWithWhileLoop2.bpmn");
//        enact("C:\\Work\\DSG\\Flexible-Process-Enactment-CEP\\src\\etc\\examples\\ProcessWithWhileLoop3.bpmn");
//        enact("C:\\Work\\DSG\\Flexible-Process-Enactment-CEP\\src\\etc\\examples\\ProcessWithTwoLoopsV11.bpmn");
//         enact("C:\\Work\\DSG\\Flexible-Process-Enactment-CEP\\src\\etc\\examples\\Hybrid Case Management example fro Tijs Paper.bpmn");
//        enactBPMN("C:\\Work\\DSG\\Flexible-Process-Enactment-CEP\\src\\etc\\examples\\Case Management example fro Tijs Paper V4.bpmn");
//        enactBPMN("C:\\Work\\DSG\\Flexible-Process-Enactment-CEP\\src\\etc\\examples\\Simple Process.bpmn");
//        testReadDCRSVG("C:\\Work\\DSG\\Flexible-Process-Enactment-CEP\\src\\etc\\examples\\DCR\\tsr-dcrgraph.svg");
//        enactDCR("C:\\Work\\DSG\\Flexible-Process-Enactment-CEP\\src\\etc\\examples\\DCR\\dcr-case-managment-hugo.xml");
//        enactDCR("C:\\Work\\DSG\\Flexible-Process-Enactment-CEP\\src\\etc\\examples\\DCR\\dcr-graph-inner-declarative-process.xml");
//        enactHybrid("C:\\Work\\DSG\\Flexible-Process-Enactment-CEP\\src\\etc\\examples\\hybrid\\hybrid.epl");
//        enactDCR("C:\\Work\\DSG\\Flexible-Process-Enactment-CEP\\src\\etc\\examples\\DCR\\all-relations-DCR.xml");
//        enactEPL("C:\\Work\\DSG\\Flexible-Process-Enactment-CEP\\src\\etc\\examples\\DCR\\dcr-case-managment-hugo.epl");
//        enactEPL("C:\\Work\\DSG\\Flexible-Process-Enactment-CEP\\src\\etc\\examples\\hybrid\\hybrid.epl");
//        enactEPL("C:\\Work\\DSG\\Flexible-Process-Enactment-CEP\\src\\etc\\examples\\Case Management example fro Tijs Paper V3.epl");
//        enactBPMN("C:\\Work\\DSG\\Flexible-Process-Enactment-CEP\\src\\etc\\examples\\Simple Process.bpmn");
//        enactBPMN("C:\\Work\\DSG\\Flexible-Process-Enactment-CEP\\src\\etc\\examples\\Simple Inclusive OR.bpmn");
//        enactEPL("C:\\Work\\DSG\\Flexible-Process-Enactment-CEP\\src\\etc\\examples\\Simple Inclusive OR.epl");
    }

    public static void obtainProcessGraph() {
        File input;

        input = new File("C:\\Work\\DSG\\Flexible-Process-Enactment-CEP\\src\\etc\\examples\\ProcessWithWhileLoop3.bpmn");

        BPMNRulesGenerator BPMNRulesGenerator = new BPMNRulesGenerator(input);

        ProcessGraph graph = BPMNRulesGenerator.buildBPMNQProcessGraph();
        System.out.println(graph.getActivities().stream().map(e -> e.getName()).collect(Collectors.toList()).toString());

    }

    public static void generateBPMNRulesToFile(String inputBPMNFile)
    {
        File input = new File(inputBPMNFile);
        BPMNRulesGenerator BPMNRulesGenerator = new BPMNRulesGenerator(input);

        String rules = BPMNRulesGenerator.generateEPLModule();
        String moduleFileName = inputBPMNFile.substring(0, inputBPMNFile.indexOf(".")) + ".epl";
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(moduleFileName));
            writer.write(rules);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }


    public static void generateRules() {
        File input;

        input = new File("C:\\Work\\DSG\\Flexible-Process-Enactment-CEP\\src\\etc\\examples\\Process222.bpmn");

        BPMNRulesGenerator BPMNRulesGenerator = new BPMNRulesGenerator(input);

//        rulesGenerator.getNodeIDs().forEach( id ->{
//            System.out.printf("Node ID:%s\n", id);
//        });
        System.out.println(BPMNRulesGenerator.generateEPLModule());
    }

    private static void enactDCR(String inputDCRXMLFile) {
        File input = new File(inputDCRXMLFile);
        try {
            DCRRuleGenerator dcrRuleGenerator = new DCRRuleGenerator(1, 1, input);
            String rules = dcrRuleGenerator.generateEPLModule();


            String moduleFileName = inputDCRXMLFile.substring(0, inputDCRXMLFile.indexOf(".")) + ".epl";

            try {
                BufferedWriter writer = new BufferedWriter(new FileWriter(moduleFileName));
                writer.write(rules);
                writer.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.exit(0);
            enactEPL(moduleFileName);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }

    }

    private static void testReadDCRSVG(String inputFile) {
        try {
            TransitionGraph.parseDCRSVGToTransitionGraph(inputFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void enactBPMN(String inputBPMNFile) {

        File input = new File(inputBPMNFile);
        BPMNRulesGenerator BPMNRulesGenerator = new BPMNRulesGenerator(input);

        String rules = BPMNRulesGenerator.generateEPLModule();
        String moduleFileName = inputBPMNFile.substring(0, inputBPMNFile.indexOf(".")) + ".epl";
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(moduleFileName));
            writer.write(rules);
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        enactEPL(moduleFileName);

    }

    private static void enactEPL(String moduleFileName) {
        EPCompiler compiler = EPCompilerProvider.getCompiler();

        Configuration configuration = new Configuration();
        configuration.getCommon().addEventType(ProcessEvent.class);

        configuration.getRuntime().getExecution().setPrioritized(true);
        configuration.getRuntime().getThreading().setInternalTimerEnabled(false);
//        configuration.getCompiler().getByteCode().setBusModifierEventType(EventTypeBusModifier.BUS);
        configuration.getCompiler().getByteCode().setAccessModifiersPublic();


        EPRuntime runtime;


        InputStream inputFile = null;
        //Runner.class.getClassLoader().getResourceAsStream("etc/examples/example-process-222.epl");
//        if (inputFile == null) {
        try {
//                inputFile = Files.newInputStream(Paths.get("C:\\Work\\DSG\\Flexible-Process-Enactment-CEP\\src\\etc\\examples\\example-process-222.epl"));
            inputFile = Files.newInputStream(Paths.get(moduleFileName));
            Module module = compiler.readModule(inputFile, moduleFileName);

//                Module module = compiler.parseModule(rules);

            CompilerArguments compArgs = new CompilerArguments(configuration);
            EPCompiled compiled = compiler.compile(module, compArgs);

            runtime = EPRuntimeProvider.getDefaultRuntime(configuration);
            runtime.initialize();

            EPDeployment dep = runtime.getDeploymentService().deploy(compiled, new DeploymentOptions().setDeploymentId("flexEnactment-222"));
            compArgs.getPath().add(runtime.getRuntimePath());

            EPEventService eventService = runtime.getEventService();


            eventService.clockExternal();
            eventService.advanceTime(0);
            EventSender sender = eventService.getEventSender("ProcessEvent");

            handleBPMNEventStream(runtime, dep, eventService);
//            handleBPMNStateTableUpdateStream(runtime,dep,eventService);

            EPStatement statement2 = runtime.getDeploymentService().getStatement(dep.getDeploymentId(), "track-case-variables");

            if (statement2 != null) {
                statement2.addListener((newData, oldData, stat, runt) -> {

                    for (int i = 0; i < newData.length; i++)
                        System.out.printf("Record of process model %d and case %d variables  %S\n", Integer.valueOf(newData[i].get("pmID").toString())
                                , Integer.valueOf(newData[i].get("caseID").toString())
                                , Integer.valueOf(newData[i].get("variables").toString()));
                });
            }
            handleDCREventStream(runtime, dep,eventService);

            Map<String, Object> variables = initializeManufacturingProcessInstanceData();
            for (int i = 1; i <= 1; i++) {
                ProcessEvent startNewProcessInstance = new ProcessEvent(3, i, "S1", /*0,*/ "started"
                        , variables, System.currentTimeMillis());

                sender.sendEvent(startNewProcessInstance);
                eventService.advanceTime(startNewProcessInstance.getTimestamp());
//                    Thread.sleep(100);
            }

        } catch (IOException | ParseException | EPCompileException | EPDeployException | EPException e) {
            e.printStackTrace();
            System.out.println(e.getMessage());
        }
    }

    private static void handleDCREventStream(EPRuntime runtime, EPDeployment dep, EPEventService eventService) {
        EPStatement statement3 = runtime.getDeploymentService().getStatement(dep.getDeploymentId(), "track-dcr-event");

        if (statement3 != null) {
            AtomicInteger lastSeen = new AtomicInteger();
            lastSeen.set(-1);
            statement3.addListener((newData, oldData, stat, runt) -> {


                System.out.println("Newly available tasks are:");

                for (int last = lastSeen.get() + 1; last < newData.length; last++) {
                    int pmID = (int) newData[last].get("ProcessModelID");
                    //      double x = (double) newData[0].get("x");
                    int caseID = (int) newData[last].get("caseID");
                    String nodeID = (String) newData[last].get("eventID");

                    System.out.printf("Option %d, %d,%d,%s\n", last,
                            pmID, caseID, nodeID);
                    lastSeen.set(last);
                }

                Scanner scanner = new Scanner(System.in);
                int choice = -1;
                while (!(0 <= choice && choice < newData.length)) {
                    System.out.print(String.format("Enter a number between 0 and %d: ", newData.length - 1));
                    choice = scanner.nextInt();
                }
                ProcessEvent dcrEvent = new ProcessEvent((int) newData[choice].get("ProcessModelID"),
                        (int) newData[choice].get("caseID"), (String) newData[choice].get("eventID"), "completed"
                        , null, System.currentTimeMillis());

                eventService.sendEventBean(dcrEvent, "ProcessEvent");
                eventService.advanceTime(dcrEvent.getTimestamp());

            });
        }
    }

    private static Map<String, Object> initializeProcessInstanceData() {
        Map<String, Object> variables = new HashMap<>();
//        variables.put("Cond1", Boolean.TRUE);
//        variables.put("Cond2", Boolean.FALSE);
//        variables.put("Cond3", Boolean.FALSE);
//        variables.put("Cond4", Boolean.TRUE);
//
//        variables.put("Cond11", Boolean.TRUE);
//        variables.put("Cond22", Boolean.FALSE);
//        variables.put("Cond33", Boolean.FALSE);
//        variables.put("Cond44", Boolean.TRUE);
        variables.put("nextAction","close");
//        variables.put("iterate",Boolean.TRUE);
//        variables.put("noiterate", Boolean.FALSE);
        variables.put("holdMeeting", Boolean.FALSE);
        variables.put("caseLocked", Boolean.FALSE);
        return variables;
    }

    private static Map<String, Object> initializeManufacturingProcessInstanceData() {
        Map<String, Object> variables = new HashMap<>();
        variables.put("service","MillingAndTurning"); // possible values are Milling, Turning, or MillingAndTurning
        return variables;
    }

    private static void handleBPMNEventStream(EPRuntime runtime, EPDeployment dep, EPEventService eventService) {
        EPStatement statement = runtime.getDeploymentService().getStatement(dep.getDeploymentId(), "track-events");

        if (statement != null) {
            statement.addListener((newData, oldData, stat, runt) -> {

                int last = newData.length - 1;

                int pmID = (int) newData[last].get("pmID");
                //      double x = (double) newData[0].get("x");
                int caseID = (int) newData[last].get("caseID");
                String nodeID = (String) newData[last].get("nodeID");
//                    int cycleNum = (int) newData[last].get("cycleNum");
                String state = (String) newData[last].get("state");
                Map<String, Object> payLoad = (Map<String, Object>) newData[last].get("payLoad");
                if (payLoad == null) {
                    payLoad = new HashMap<String, Object>();
                    payLoad.put("None", "none");
                }
                long time = (long) newData[last].get("timestamp");
//                    System.out.printf("A new process event received with Process Model ID:%d," +
//                                    " Case ID:%d, Node ID:%s, Cycle Number:%d, State:%s, Payload:%s, and Time:%d%n",
//                            pmID, caseID, nodeID,cycleNum,state,payLoad.toString(), time);
//                if (!state.equals("skipped"))
                    System.out.printf("%d,%d,%s, %s, %s, %d\n",
                        pmID, caseID, nodeID, state, payLoad.toString().replace(",", ";"), time);

                // check the state table content

//                String query = "select * from Execution_State order by timestamp desc";
//                CompilerArguments compilerArguments = new CompilerArguments();
//                compilerArguments.getPath().add(runtime.getRuntimePath());
//                EPCompiled compiled = null;
//                try {
//                    compiled = EPCompilerProvider.getCompiler().compileQuery(query, compilerArguments);
//                    EPFireAndForgetQueryResult result = runtime.getFireAndForgetService().executeQuery(compiled);
//                    System.out.println("State table size "+ result.getArray().length);
//                    for (EventBean row : result.getArray()) {
//                        System.out.printf("State table content --- %d, %d, %s, %s, %d\n", (int) row.get("pmID"), (int) row.get("caseID"), row.get("nodeID"), row.get("state"), (long)row.get("timestamp"));
//                    }
//                } catch (EPCompileException e) {
//                    throw new RuntimeException(e);
//                }



//                    Handler for Activities
                if (nodeID.equals("A") && state.equals("started")) {
                    handleActivityA(eventService, pmID, caseID, nodeID, payLoad);

                } else if (nodeID.equals("B") && state.equals("started")) {
                    handleActivityB(eventService, pmID, caseID, nodeID, payLoad);

                } else if (nodeID.equals("BB") && state.equals("started")) {
                    handleActivityBB(eventService, pmID, caseID, nodeID, payLoad);

                } else if (nodeID.equals("C") && state.equals("started")) {
                    handleActivityC(eventService, pmID, caseID, nodeID, payLoad);

                } else if (nodeID.equals("CC") && state.equals("started")) {
                    handleActivityCC(eventService, pmID, caseID, nodeID, payLoad);

                } else if (nodeID.equals("D") && state.equals("started")) {
                    handleActivityD(eventService, pmID, caseID, nodeID, payLoad);

                } else if (nodeID.equals("E") && state.equals("started")) {
                    handleActivityE(eventService, pmID, caseID, nodeID, payLoad);

                } else if (nodeID.equals("F") && state.equals("started")) {
                    handleActivityF(eventService, pmID, caseID, nodeID, payLoad);

                } else if (nodeID.equals("FF") && state.equals("started")) {
                    handleActivityF(eventService, pmID, caseID, nodeID, payLoad);

                } else if (nodeID.equals("Decide what to do next") && state.equals("started")) {
                    handleActivityDecideWhatToDoNext(eventService, pmID, caseID, nodeID, payLoad);

                 } else if (nodeID.equals("Schedule meeting") && state.equals("started")) {
                    handleActivityScheduleMeeting(eventService, pmID, caseID, nodeID, payLoad);

                } else if (nodeID.equals("Hold meeting") && state.equals("started")) {
                    handleActivityHoldMeeting(eventService, pmID, caseID, nodeID, payLoad);

                } else if (nodeID.equals("Lock case") && state.equals("started")) {
                    handleActivityLockCase(eventService, pmID, caseID, nodeID, payLoad);

                } else if (nodeID.equals("Declarative part") && state.equals("started")) {
                    System.out.println("Now the declarative part is  kicking in...");
                } else if (nodeID.equals("Fina Inspection Q.C.") && state.equals("started")){
                    handleActivityFinalInspectionQC(eventService,pmID,caseID,nodeID,payLoad);

                } else if (nodeID.equals("Lapping") && state.equals("started")) {
                    handleActivityLapping(eventService, pmID, caseID, nodeID, payLoad);

                } else if (nodeID.equals("Laser Marking") && state.equals("started")) {
                    handleActivityLaserMarking(eventService, pmID, caseID, nodeID, payLoad);

                } else if (nodeID.equals("Turning and milling") && state.equals("started")) {
                    handleActivityTurningAndMilling(eventService, pmID, caseID, nodeID, payLoad);

                } else if (nodeID.equals("Turning and milling Q.C.") && state.equals("started")) {
                    handleActivityTurningAndMillingQC(eventService, pmID, caseID, nodeID, payLoad);

                } else if (nodeID.equals("Round Grinding") && state.equals("started")) {
                    handleActivityRoundGrinding(eventService, pmID, caseID, nodeID, payLoad);

                } else if (nodeID.equals("Packing") && state.equals("started")) {
                    handleActivityPacking(eventService, pmID, caseID, nodeID, payLoad);

                } else if (state.equals("started")) {
                    if (nodeID.startsWith("S1"))
                        return;
                    handleGeneralActivity(eventService, pmID, caseID, nodeID, payLoad);
                }

            });
        }
    }

    private static void handleBPMNStateTableUpdateStream(EPRuntime runtime, EPDeployment dep, EPEventService eventService) {
        EPStatement statement = runtime.getDeploymentService().getStatement(dep.getDeploymentId(), "track-state-table");

        if (statement != null) {
            statement.addListener((newData, oldData, stat, runt) -> {

                int last = newData.length - 1;

                int pmID = (int) newData[last].get("pmID");
                //      double x = (double) newData[0].get("x");
                int caseID = (int) newData[last].get("caseID");
                String nodeID = (String) newData[last].get("nodeID");
//                    int cycleNum = (int) newData[last].get("cycleNum");
                String state = (String) newData[last].get("state");

                long time = (long) newData[last].get("timestamp");
//                    System.out.printf("A new process event received with Process Model ID:%d," +
//                                    " Case ID:%d, Node ID:%s, Cycle Number:%d, State:%s, Payload:%s, and Time:%d%n",
//                            pmID, caseID, nodeID,cycleNum,state,payLoad.toString(), time);
                System.out.printf("%d,%d,%s, %s, %d\n",
                            pmID, caseID, nodeID, state, time);

            });
        }
    }

    private static void handleActivityScheduleMeeting(EPEventService sender, int pmID, int caseID, String nodeID, Map<String, Object> payLoad){
        Map<String, Object> variables = new HashMap<>();


        for (String k : payLoad.keySet())
            variables.put(k, payLoad.get(k));
        variables.put("holdMeeting",Boolean.TRUE);

        ProcessEvent activityACompleted = new ProcessEvent(pmID, caseID, nodeID, "completed", variables, System.currentTimeMillis());
        sender.sendEventBean(activityACompleted, "ProcessEvent");
        sender.advanceTime(activityACompleted.getTimestamp());

    }

    private static void handleActivityHoldMeeting(EPEventService sender, int pmID, int caseID, String nodeID, Map<String, Object> payLoad){
        Map<String, Object> variables = new HashMap<>();


        for (String k : payLoad.keySet())
            variables.put(k, payLoad.get(k));
        variables.put("holdMeeting",Boolean.FALSE);

        ProcessEvent activityACompleted = new ProcessEvent(pmID, caseID, nodeID, "completed", variables, System.currentTimeMillis());
        sender.sendEventBean(activityACompleted, "ProcessEvent");
        sender.advanceTime(activityACompleted.getTimestamp());

    }

    private static void handleActivityLockCase(EPEventService sender, int pmID, int caseID, String nodeID, Map<String, Object> payLoad){
        Map<String, Object> variables = new HashMap<>();


        for (String k : payLoad.keySet())
            variables.put(k, payLoad.get(k));
        variables.put("caseLocked",Boolean.TRUE);

        ProcessEvent activityACompleted = new ProcessEvent(pmID, caseID, nodeID, "completed", variables, System.currentTimeMillis());
        sender.sendEventBean(activityACompleted, "ProcessEvent");
        sender.advanceTime(activityACompleted.getTimestamp());

    }
    private static void handleActivityTurningAndMilling(EPEventService sender, int pmID, int caseID, String nodeID, Map<String, Object> payLoad){
        Map<String, Object> variables = new HashMap<>();


        for (String k : payLoad.keySet())
            variables.put(k, payLoad.get(k));
        System.out.println("Choose one option");

        variables.put("TMNext","TMQC"); // possible choices TMQC or TM (looping)

        System.out.println("1 - Turning and Milling (redo the task)");
        System.out.println("2 - Turning and Milling Q.C.");

        Scanner scanner = new Scanner(System.in);
        int choice =-1;

        System.out.print(String.format("Enter a number between 1 and %d: ", 2));
        choice = scanner.nextInt();

        if (choice ==1)
            variables.put("TMNext","TM");
        else if (choice ==2)
            variables.put("TMNext", "TMQC");



        double v = Math.random();
        try {
            Thread.sleep((long) (v * 1000));
            ProcessEvent activityACompleted = new ProcessEvent(pmID, caseID, nodeID, "completed", variables, System.currentTimeMillis());
            sender.sendEventBean(activityACompleted, "ProcessEvent");
            sender.advanceTime(activityACompleted.getTimestamp());
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private static void handleActivityTurningAndMillingQC(EPEventService sender, int pmID, int caseID, String nodeID, Map<String, Object> payLoad){
        Map<String, Object> variables = new HashMap<>();


        for (String k : payLoad.keySet())
            variables.put(k, payLoad.get(k));
        variables.put("TMQCNext","NQC"); // possible choices TMQC (looping),  LM, or NQC


        System.out.println("1 - Turning and Milling Q.C. (redo the task)");
        System.out.println("2 - Laser Marking");
        System.out.println("3 - Nitration Q.C.");

        Scanner scanner = new Scanner(System.in);
        int choice =-1;

        System.out.print(String.format("Enter a number between 1 and %d: ", 3));
        choice = scanner.nextInt();

        if (choice ==1)
            variables.put("TMQCNext","TMQC");
        else if (choice ==2)
            variables.put("TMQCNext", "LM");
        else if (choice ==2)
            variables.put("TMQCNext", "NQC");



        double v = Math.random();
        try {
            Thread.sleep((long) (v * 1000));
            ProcessEvent activityACompleted = new ProcessEvent(pmID, caseID, nodeID, "completed", variables, System.currentTimeMillis());
            sender.sendEventBean(activityACompleted, "ProcessEvent");
            sender.advanceTime(activityACompleted.getTimestamp());
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private static void handleActivityLaserMarking(EPEventService sender, int pmID, int caseID, String nodeID, Map<String, Object> payLoad){
        Map<String, Object> variables = new HashMap<>();


        for (String k : payLoad.keySet())
            variables.put(k, payLoad.get(k));
        variables.put("LMNext","FG"); // possible choices FG,  Lapping, or FIQC

        System.out.println("1 - Flat Grinding");
        System.out.println("2 - Lapping");
        System.out.println("3 - Final Inspection Q.C.");

        Scanner scanner = new Scanner(System.in);
        int choice =-1;

        System.out.print(String.format("Enter a number between 1 and %d: ", 3));
        choice = scanner.nextInt();

        if (choice ==1)
            variables.put("LMNext","FG");
        else if (choice ==2)
            variables.put("LMNext", "Lapping");
        else if (choice ==2)
            variables.put("LMNext", "FIQC");



        double v = Math.random();
        try {
            Thread.sleep((long) (v * 1000));
            ProcessEvent activityACompleted = new ProcessEvent(pmID, caseID, nodeID, "completed", variables, System.currentTimeMillis());
            sender.sendEventBean(activityACompleted, "ProcessEvent");
            sender.advanceTime(activityACompleted.getTimestamp());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void handleActivityLapping(EPEventService sender, int pmID, int caseID, String nodeID, Map<String, Object> payLoad){
        Map<String, Object> variables = new HashMap<>();


        for (String k : payLoad.keySet())
            variables.put(k, payLoad.get(k));
        variables.put("LappingNext","RG"); // possible choices RG,  Lapping, or FIQC

        System.out.println("1 - Round Grinding");
        System.out.println("2 - Lapping (redo the task)");
        System.out.println("3 - Final Inspection Q.C.");

        Scanner scanner = new Scanner(System.in);
        int choice =-1;

        System.out.print(String.format("Enter a number between 1 and %d: ", 3));
        choice = scanner.nextInt();

        if (choice ==1)
            variables.put("LappingNext","RG");
        else if (choice ==2)
            variables.put("LappingNext", "Lapping");
        else if (choice ==2)
            variables.put("LappingNext", "FIQC");



        double v = Math.random();
        try {
            Thread.sleep((long) (v * 1000));
            ProcessEvent activityACompleted = new ProcessEvent(pmID, caseID, nodeID, "completed", variables, System.currentTimeMillis());
            sender.sendEventBean(activityACompleted, "ProcessEvent");
            sender.advanceTime(activityACompleted.getTimestamp());
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void handleActivityRoundGrinding(EPEventService sender, int pmID, int caseID, String nodeID, Map<String, Object> payLoad){
        Map<String, Object> variables = new HashMap<>();


        for (String k : payLoad.keySet())
            variables.put(k, payLoad.get(k));
        variables.put("RGNext","FIQC"); // possible choices FIQC or RG

        System.out.println("1 - Round Grinding (redo the task)");
        System.out.println("2 - Final Inspection Q.C.");

        Scanner scanner = new Scanner(System.in);
        int choice =-1;

        System.out.print(String.format("Enter a number between 1 and %d: ", 2));
        choice = scanner.nextInt();

        if (choice ==1)
            variables.put("RGNext","RG");
        else if (choice ==2)
            variables.put("RGNext", "FIQC");




        double v = Math.random();
        try {
            Thread.sleep((long) (v * 1000));
            ProcessEvent activityACompleted = new ProcessEvent(pmID, caseID, nodeID, "completed", variables, System.currentTimeMillis());
            sender.sendEventBean(activityACompleted, "ProcessEvent");
            sender.advanceTime(activityACompleted.getTimestamp());
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private static void handleActivityPacking(EPEventService sender, int pmID, int caseID, String nodeID, Map<String, Object> payLoad){
        Map<String, Object> variables = new HashMap<>();


        for (String k : payLoad.keySet())
            variables.put(k, payLoad.get(k));
        variables.put("PackingNext","End"); // possible choices Packing or End

        System.out.println("1 - Packing (redo the task)");
        System.out.println("2 - Proceed to end");

        Scanner scanner = new Scanner(System.in);
        int choice =-1;

        System.out.print(String.format("Enter a number between 1 and %d: ", 2));
        choice = scanner.nextInt();

        if (choice ==1)
            variables.put("PackingNext","Packing");
        else if (choice ==2)
            variables.put("PackingNext", "End");




        double v = Math.random();
        try {
            Thread.sleep((long) (v * 1000));
            ProcessEvent activityACompleted = new ProcessEvent(pmID, caseID, nodeID, "completed", variables, System.currentTimeMillis());
            sender.sendEventBean(activityACompleted, "ProcessEvent");
            sender.advanceTime(activityACompleted.getTimestamp());
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
    private static void handleActivityFinalInspectionQC(EPEventService sender, int pmID, int caseID, String nodeID, Map<String, Object> payLoad){
        Map<String, Object> variables = new HashMap<>();


        for (String k : payLoad.keySet())
            variables.put(k, payLoad.get(k));
        variables.put("FIQCNext","End"); // possible choices End or GR

        System.out.println("1 - Proceed to end");
        System.out.println("2 - Grinding Rework (loop)");

        Scanner scanner = new Scanner(System.in);
        int choice =-1;

        System.out.print(String.format("Enter a number between 1 and %d: ", 2));
        choice = scanner.nextInt();

        if (choice ==1)
            variables.put("FIQCNext","End");
        else if (choice ==2)
            variables.put("FIQCNext", "GR");




        double v = Math.random();
        try {
            Thread.sleep((long) (v * 1000));
            ProcessEvent activityACompleted = new ProcessEvent(pmID, caseID, nodeID, "completed", variables, System.currentTimeMillis());
            sender.sendEventBean(activityACompleted, "ProcessEvent");
            sender.advanceTime(activityACompleted.getTimestamp());
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private static void handleActivityDecideWhatToDoNext(EPEventService sender, int pmID, int caseID, String nodeID, Map<String, Object> payLoad) {
        Map<String, Object> variables = new HashMap<>();


        for (String k : payLoad.keySet())
            variables.put(k, payLoad.get(k));

        System.out.println("Choose one option");
        if (variables.get("caseLocked").equals(Boolean.FALSE))
            System.out.println("1 - Upload document");
        System.out.println("2 - Search documents");
        System.out.println("3 - Download document");
        if (variables.get("holdMeeting").equals(Boolean.FALSE))
            System.out.println("4 - Schedule meeting");
        if (variables.get("holdMeeting").equals(Boolean.TRUE))
            System.out.println("5 - Hold meeting");

        System.out.println("6 - Lock case");
        System.out.println("7 - Close case");
        Scanner scanner = new Scanner(System.in);
        int choice =-1;

        System.out.print(String.format("Enter a number between 1 and %d: ", 7));
        choice = scanner.nextInt();

        if (choice ==1)
            variables.put("nextAction","upload");
        else if (choice ==2)
            variables.put("nextAction", "search");
        else if (choice == 3)
            variables.put("nextAction","download");
        else if (choice ==4)
            variables.put("nextAction", "schedule");
        else if (choice ==5)
            variables.put("nextAction","hold");
        else if (choice ==6)
            variables.put("nextAction","lock");
        else if (choice ==7)
            variables.put("nextAction","close");

        if (choice < 7)
        {
            variables.put("iterate", Boolean.TRUE);
            variables.put("noiterate", Boolean.FALSE);
        }
        else
        {
            variables.put("noiterate", Boolean.TRUE);
            variables.put("iterate", Boolean.FALSE);
        }


        double v = Math.random();
        try {
            Thread.sleep((long) (v * 1000));
            ProcessEvent activityACompleted = new ProcessEvent(pmID, caseID, nodeID, "completed", variables, System.currentTimeMillis());
            sender.sendEventBean(activityACompleted, "ProcessEvent");
            sender.advanceTime(activityACompleted.getTimestamp());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void enactHybrid(String EPLModuleFile)
    {
        enactEPL(EPLModuleFile);
    }
    private static void handleGeneralActivity(EPEventService sender, int pmID, int caseID, String nodeID, Map<String, Object> payLoad) {

        Map<String, Object> variables = new HashMap<>();

        for (String k : payLoad.keySet())
            variables.put(k, payLoad.get(k));
        double v = Math.random();

        try {
            Thread.sleep((long) (v * 10000));
            ProcessEvent activityACompleted = new ProcessEvent(pmID, caseID, nodeID, "completed", variables, System.currentTimeMillis());
            sender.sendEventBean(activityACompleted, "ProcessEvent");
            sender.advanceTime(activityACompleted.getTimestamp());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void handleActivityA(EPEventService sender, int pmID, int caseID, String nodeID,/* int cycleNum,*/ Map<String, Object> payLoad) {

        Map<String, Object> variables = new HashMap<>();

        for (String k : payLoad.keySet())
            variables.put(k, payLoad.get(k));
//        variables.put("cond3", Boolean.FALSE);
        double v = Math.random();
        if (v < 0.25) {
            variables.put("Cond1", Boolean.TRUE);
            variables.put("Cond2", Boolean.TRUE);

        } else if (v < 0.5) {
            variables.put("Cond1", Boolean.TRUE);
            variables.put("Cond2", Boolean.FALSE);
        } else if (v < 0.75) {
            variables.put("Cond1", Boolean.FALSE);
            variables.put("Cond2", Boolean.TRUE);
        } else {
            variables.put("Cond1", Boolean.FALSE);
            variables.put("Cond2", Boolean.FALSE);
        }
        variables.put("Cond1", Boolean.TRUE);
        variables.put("Cond2", Boolean.FALSE);

        variables.put("Cond11", Boolean.TRUE);
        variables.put("Cond22", Boolean.FALSE);
        try {
            Thread.sleep((long) (v * 10000));
            ProcessEvent activityACompleted = new ProcessEvent(pmID, caseID, nodeID, "completed", variables, System.currentTimeMillis());
            sender.sendEventBean(activityACompleted, "ProcessEvent");
            sender.advanceTime(activityACompleted.getTimestamp());
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private static void handleActivityB(EPEventService sender, int pmID, int caseID, String nodeID, Map<String, Object> payLoad) {
        Map<String, Object> variables = new HashMap<>();

        for (String k : payLoad.keySet())
            variables.put(k, payLoad.get(k));
        double v = Math.random();
        if (v < 0.5) {
            variables.put("Cond3", Boolean.TRUE);
            variables.put("Cond4", Boolean.FALSE);


        } else {
            variables.put("Cond3", Boolean.FALSE);
            variables.put("Cond4", Boolean.TRUE);
        }

//        variables.put("Cond3", Boolean.FALSE);
//        variables.put("Cond4", Boolean.TRUE);
        try {
            Thread.sleep((long) (v * 1000));
            ProcessEvent activityACompleted = new ProcessEvent(pmID, caseID, nodeID, "completed", variables, System.currentTimeMillis());
            sender.sendEventBean(activityACompleted, "ProcessEvent");
            sender.advanceTime(activityACompleted.getTimestamp());
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private static void handleActivityBB(EPEventService sender, int pmID, int caseID, String nodeID, Map<String, Object> payLoad) {
        Map<String, Object> variables = new HashMap<>();

        for (String k : payLoad.keySet())
            variables.put(k, payLoad.get(k));
        double v = Math.random();
        if (v < 0.5) {
            variables.put("Cond33", Boolean.TRUE);
            variables.put("Cond44", Boolean.FALSE);


        } else {
            variables.put("Cond33", Boolean.FALSE);
            variables.put("Cond44", Boolean.TRUE);

        }

        variables.put("Cond33", Boolean.FALSE);
        variables.put("Cond44", Boolean.TRUE);
        try {
            Thread.sleep((long) (v * 1000));
            ProcessEvent activityACompleted = new ProcessEvent(pmID, caseID, nodeID, "completed", variables, System.currentTimeMillis());
            sender.sendEventBean(activityACompleted, "ProcessEvent");
            sender.advanceTime(activityACompleted.getTimestamp());
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private static void handleActivityC(EPEventService sender, int pmID, int caseID, String nodeID, Map<String, Object> payLoad) {
        Map<String, Object> variables = new HashMap<>();

        for (String k : payLoad.keySet())
            variables.put(k, payLoad.get(k));
        double v = Math.random();
        if (v < 0.25) {
            variables.put("Cond1", Boolean.TRUE);
            variables.put("Cond2", Boolean.TRUE);

        } else if ( v < 0.5){
            variables.put("Cond1", Boolean.TRUE);
            variables.put("Cond2", Boolean.FALSE);
        }
        else if (v < 0.75) {
            variables.put("Cond1", Boolean.FALSE);
            variables.put("Cond2", Boolean.TRUE);
        }
        else
        {
            variables.put("Cond1", Boolean.FALSE);
            variables.put("Cond2", Boolean.FALSE);
        }
        variables.put("Cond1", Boolean.TRUE);
        variables.put("Cond2", Boolean.FALSE);
        try {
            Thread.sleep((long) (v * 1000));
            ProcessEvent activityACompleted = new ProcessEvent(pmID, caseID, nodeID, "completed", variables, System.currentTimeMillis());
            sender.sendEventBean(activityACompleted, "ProcessEvent");
            sender.advanceTime(activityACompleted.getTimestamp());
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private static void handleActivityCC(EPEventService sender, int pmID, int caseID, String nodeID, Map<String, Object> payLoad) {
        Map<String, Object> variables = new HashMap<>();

        for (String k : payLoad.keySet())
            variables.put(k, payLoad.get(k));
        double v = Math.random();
        if (v < 0.5) {
            variables.put("Cond33", Boolean.TRUE);
            variables.put("Cond44", Boolean.FALSE);

        } else {
            variables.put("Cond33", Boolean.FALSE);
            variables.put("Cond44", Boolean.TRUE);
        }

        variables.put("3Cond3", Boolean.FALSE);
        variables.put("Cond44", Boolean.TRUE);
        try {
            Thread.sleep((long) (v * 1000));
            ProcessEvent activityACompleted = new ProcessEvent(pmID, caseID, nodeID, "completed", variables, System.currentTimeMillis());
            sender.sendEventBean(activityACompleted, "ProcessEvent");
            sender.advanceTime(activityACompleted.getTimestamp());
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private static void handleActivityD(EPEventService sender, int pmID, int caseID, String nodeID, Map<String, Object> payLoad) {
        Map<String, Object> variables = new HashMap<>();

        for (String k : payLoad.keySet())
            variables.put(k, payLoad.get(k));
        double v = Math.random();
        if (v < 0.5) {
            variables.put("Cond4", Boolean.TRUE);


        } else {
            variables.put("Cond4", Boolean.FALSE);

        }

//        variables.put("cond3", Boolean.FALSE);
        try {
            Thread.sleep((long) (v * 1000));
            ProcessEvent activityACompleted = new ProcessEvent(pmID, caseID, nodeID, "completed", variables, System.currentTimeMillis());
            sender.sendEventBean(activityACompleted, "ProcessEvent");
            sender.advanceTime(activityACompleted.getTimestamp());
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private static void handleActivityE(EPEventService sender, int pmID, int caseID, String nodeID, Map<String, Object> payLoad) {
        Map<String, Object> variables = new HashMap<>();

        for (String k : payLoad.keySet())
            variables.put(k, payLoad.get(k));
        double v = Math.random();
//        if ( v < 0.5) {
//            variables.put("Cond4", Boolean.TRUE);
//
//
//        }
//        else
//        {
//            variables.put("Cond4", Boolean.FALSE);
//
//        }

//        variables.put("cond3", Boolean.FALSE);
        try {
            Thread.sleep((long) (v * 1000));
            ProcessEvent activityACompleted = new ProcessEvent(pmID, caseID, nodeID, "completed", variables, System.currentTimeMillis());
            sender.sendEventBean(activityACompleted, "ProcessEvent");
            sender.advanceTime(activityACompleted.getTimestamp());
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private static void handleActivityF(EPEventService sender, int pmID, int caseID, String nodeID, Map<String, Object> payLoad) {

        double v = Math.random();

        try {
            Thread.sleep((long) (v * 1000));
            ProcessEvent activityACompleted = new ProcessEvent(pmID, caseID, nodeID, "completed", payLoad, System.currentTimeMillis());
            sender.sendEventBean(activityACompleted, "ProcessEvent");
            sender.advanceTime(activityACompleted.getTimestamp());
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
