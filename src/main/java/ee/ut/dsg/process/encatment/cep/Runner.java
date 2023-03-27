package ee.ut.dsg.process.encatment.cep;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.bpmnq.ProcessGraph;
import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.EPException;
import com.espertech.esper.common.client.EventSender;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.module.Module;
import com.espertech.esper.common.client.module.ParseException;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.EPCompiler;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.runtime.client.*;
import ee.ut.dsg.process.encatment.cep.events.ProcessEvent;


public class Runner {

    public static void main(String[] args) {

//        obtainProcessGraph();
//        System.exit(0);
//        generateRules();
//        System.exit(0);
        enact("C:\\Work\\DSG\\Flexible-Process-Enactment-CEP\\src\\etc\\examples\\Process222V6.bpmn");



    }

    public static void obtainProcessGraph()
    {
        File input;

        input = new File("C:\\Work\\DSG\\Flexible-Process-Enactment-CEP\\src\\etc\\examples\\Process222.bpmn");

        RulesGenerator rulesGenerator = new RulesGenerator(input);

        ProcessGraph graph = rulesGenerator.buildBPMNQProcessGraph();
        System.out.println(graph.getActivities().stream().map( e -> e.getName()).collect(Collectors.toList()).toString());

    }
    public static void generateRules()
    {
        File input;

        input = new File("C:\\Work\\DSG\\Flexible-Process-Enactment-CEP\\src\\etc\\examples\\Process222.bpmn");

        RulesGenerator rulesGenerator = new RulesGenerator(input);

//        rulesGenerator.getNodeIDs().forEach( id ->{
//            System.out.printf("Node ID:%s\n", id);
//        });
        System.out.println(rulesGenerator.generateEPLModule());
    }
    private static void enact(String inputBPMNFile) {

        File input = new File(inputBPMNFile);
        RulesGenerator rulesGenerator = new RulesGenerator(input);

        String rules = rulesGenerator.generateEPLModule();
        String moduleFileName = inputBPMNFile.substring(0, inputBPMNFile.indexOf(".")) + ".epl";
//        try {
//            BufferedWriter writer = new BufferedWriter(new FileWriter(moduleFileName));
//            writer.write(rules);
//            writer.close();
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

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
                Module module = compiler.readModule(inputFile, "example-process-222.epl");

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

                EPStatement statement = runtime.getDeploymentService().getStatement(dep.getDeploymentId(), "track-events");

                statement.addListener((newData, oldData, stat, runt) -> {

                    int last = newData.length-1;

                    int pmID = (int) newData[last].get("pmID");
                    //      double x = (double) newData[0].get("x");
                    int caseID = (int) newData[last].get("caseID");
                    String nodeID = (String) newData[last].get("nodeID");
                    int cycleNum = (int) newData[last].get("cycleNum");
                    String state = (String) newData[last].get("state");
                    Map<String, Object> payLoad = (Map<String, Object>) newData[last].get("payLoad");
                    long time = (long) newData[last].get("timestamp");
//                    System.out.printf("A new process event received with Process Model ID:%d," +
//                                    " Case ID:%d, Node ID:%s, Cycle Number:%d, State:%s, Payload:%s, and Time:%d%n",
//                            pmID, caseID, nodeID,cycleNum,state,payLoad.toString(), time);
                    System.out.printf("%d,%d,%s, %d, %s, %s, %d\n",
                            pmID, caseID, nodeID,cycleNum,state,payLoad.toString().replace(",",";"), time);

//                    Handler for Activities
                    if (nodeID.equals("A") && state.equals("started"))
                    {
                        handleActivityA(eventService, pmID, caseID, nodeID, cycleNum, payLoad);

                    }
                    else if (nodeID.equals("B") && state.equals("started"))
                    {
                        handleActivityB(eventService, pmID, caseID, nodeID, cycleNum, payLoad);

                    }
                    else if (nodeID.equals("C") && state.equals("started"))
                    {
                        handleActivityC(eventService, pmID, caseID, nodeID, cycleNum, payLoad);

                    }
                    else if (nodeID.equals("D") && state.equals("started"))
                    {
                        handleActivityD(eventService, pmID, caseID, nodeID, cycleNum, payLoad);

                    }
                    else if (nodeID.equals("F") && state.equals("started"))
                    {
                        handleActivityF(eventService, pmID, caseID, nodeID, cycleNum, payLoad);

                    }
                });


//                EPStatement statement2 = runtime.getDeploymentService().getStatement(dep.getDeploymentId(), "Execution-History");
//
//                statement2.addListener((newData, oldData, stat, runt) ->{
//
//                    for (int i = 0; i < newData.length;i++)
//                        System.out.printf("Number of events for case 1 is %d\n", Integer.valueOf(newData[i].get("pmID").toString()));
//                });
                Map<String, Object> variables = new HashMap<>();
                variables.put("Cond1", Boolean.TRUE);
                variables.put("Cond2", Boolean.TRUE);
                variables.put("Cond3", Boolean.FALSE);
                variables.put("Cond4", Boolean.TRUE);
                for (int i = 1; i <=1; i++) {
                    ProcessEvent startNewProcessInstance = new ProcessEvent(1, i, "SE1", 0, "completed"
                            , variables, System.currentTimeMillis());

                    sender.sendEvent(startNewProcessInstance);
                    eventService.advanceTime(startNewProcessInstance.getTimestamp());
//                    Thread.sleep(100);
                }

            } catch (IOException | ParseException | EPCompileException | EPDeployException | EPException e) {
                e.printStackTrace();
                System.out.println(e.getMessage());
            }
//        }
    }

    private static void handleActivityA(EPEventService sender, int pmID, int caseID, String nodeID, int cycleNum, Map<String, Object> payLoad) {

        Map<String, Object> variables = new HashMap<>();

        for (String k :payLoad.keySet())
            variables.put(k, payLoad.get(k));
//        variables.put("cond3", Boolean.FALSE);
        double v =Math.random();
        if ( v < 0.25) {
            variables.put("cond1", Boolean.TRUE);
            variables.put("cond2", Boolean.TRUE);

        }
        else if (v < 0.5)
        {
            variables.put("cond1", Boolean.TRUE);
            variables.put("cond2", Boolean.FALSE);
        }
        else if (v < 0.75)
        {
            variables.put("cond1", Boolean.FALSE);
            variables.put("cond2", Boolean.TRUE);
        }
        else
        {
            variables.put("cond1", Boolean.FALSE);
            variables.put("cond2", Boolean.FALSE);
        }
//        variables.put("cond1", Boolean.TRUE);
        try {
            Thread.sleep((long) (v*10000));
            ProcessEvent activityACompleted = new ProcessEvent(pmID, caseID, nodeID, cycleNum,"completed",variables,System.currentTimeMillis());
            sender.sendEventBean(activityACompleted, "ProcessEvent");
            sender.advanceTime(activityACompleted.getTimestamp());
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private static void handleActivityB(EPEventService sender, int pmID, int caseID, String nodeID, int cycleNum, Map<String, Object> payLoad) {
        Map<String, Object> variables = new HashMap<>();

        for (String k :payLoad.keySet())
            variables.put(k, payLoad.get(k));
        double v =Math.random();
        if ( v < 0.5) {
            variables.put("cond3", Boolean.TRUE);


        }
        else
        {
            variables.put("cond3", Boolean.FALSE);

        }

//        variables.put("cond3", Boolean.TRUE);
        try {
            Thread.sleep((long) (v*1000));
            ProcessEvent activityACompleted = new ProcessEvent(pmID, caseID, nodeID, cycleNum,"completed",variables,System.currentTimeMillis());
            sender.sendEventBean(activityACompleted,"ProcessEvent");
            sender.advanceTime(activityACompleted.getTimestamp());
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private static void handleActivityC(EPEventService sender, int pmID, int caseID, String nodeID, int cycleNum, Map<String, Object> payLoad) {
        Map<String, Object> variables = new HashMap<>();

        for (String k :payLoad.keySet())
            variables.put(k, payLoad.get(k));
        double v =Math.random();
        if ( v < 0.5) {
            variables.put("cond3", Boolean.TRUE);


        }
        else
        {
            variables.put("cond3", Boolean.FALSE);

        }

//        variables.put("cond3", Boolean.FALSE);
        try {
            Thread.sleep((long) (v*1000));
            ProcessEvent activityACompleted = new ProcessEvent(pmID, caseID, nodeID, cycleNum,"completed",variables,System.currentTimeMillis());
            sender.sendEventBean(activityACompleted,"ProcessEvent");
            sender.advanceTime(activityACompleted.getTimestamp());
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private static void handleActivityD(EPEventService sender, int pmID, int caseID, String nodeID, int cycleNum, Map<String, Object> payLoad) {
        Map<String, Object> variables = new HashMap<>();

        for (String k :payLoad.keySet())
            variables.put(k, payLoad.get(k));
        double v =Math.random();
        if ( v < 0.5) {
            variables.put("cond4", Boolean.TRUE);


        }
        else
        {
            variables.put("cond4", Boolean.FALSE);

        }

//        variables.put("cond3", Boolean.FALSE);
        try {
            Thread.sleep((long) (v*1000));
            ProcessEvent activityACompleted = new ProcessEvent(pmID, caseID, nodeID, cycleNum,"completed",variables,System.currentTimeMillis());
            sender.sendEventBean(activityACompleted,"ProcessEvent");
            sender.advanceTime(activityACompleted.getTimestamp());
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    private static void handleActivityF(EPEventService sender, int pmID, int caseID, String nodeID, int cycleNum, Map<String, Object> payLoad) {

        double v =Math.random();

        try {
            Thread.sleep((long) (v*1000));
            ProcessEvent activityACompleted = new ProcessEvent(pmID, caseID, nodeID, cycleNum,"completed",payLoad,System.currentTimeMillis());
            sender.sendEventBean(activityACompleted,"ProcessEvent");
            sender.advanceTime(activityACompleted.getTimestamp());
        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
