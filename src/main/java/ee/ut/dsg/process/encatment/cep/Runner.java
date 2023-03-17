package ee.ut.dsg.process.encatment.cep;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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


        EPCompiler compiler = EPCompilerProvider.getCompiler();

        Configuration configuration = new Configuration();
        configuration.getCommon().addEventType(ProcessEvent.class);

        configuration.getRuntime().getExecution().setPrioritized(true);
        configuration.getRuntime().getThreading().setInternalTimerEnabled(true);
//        configuration.getCompiler().getByteCode().setBusModifierEventType(EventTypeBusModifier.BUS);
        configuration.getCompiler().getByteCode().setAccessModifiersPublic();


        EPRuntime runtime;


        InputStream inputFile = Runner.class.getClassLoader().getResourceAsStream("etc/examples/example-process-222.epl");
        if (inputFile == null) {
            try {
                inputFile = new FileInputStream("C:\\Work\\DSG\\Flexible-Process-Enactment-CEP\\src\\etc\\examples\\example-process-222.epl");
                Module module = compiler.readModule(inputFile, "example-process-222.epl");

                CompilerArguments compArgs = new CompilerArguments(configuration);
                EPCompiled compiled = compiler.compile(module, compArgs);

                runtime = EPRuntimeProvider.getDefaultRuntime(configuration);
                runtime.initialize();

                EPDeployment dep = runtime.getDeploymentService().deploy(compiled, new DeploymentOptions().setDeploymentId("leftObject"));
                compArgs.getPath().add(runtime.getRuntimePath());

                EPEventService eventService = runtime.getEventService();



                eventService.clockExternal();
                eventService.advanceTime(0);
                EventSender sender = eventService.getEventSender("ProcessEvent");

//                for (int i =0; i < 100; i++)
//                {

//                }
//                eventService.advanceTime(startNewProcessInstance.getTimestamp());

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
                    System.out.println(String.format("A new process event received with Process Model ID:%d," +
                            " Case ID:%d, Node ID:%s, Cycle Number:%d, State:%s, Payload:%s, and Time:%d",
                            pmID, caseID, nodeID,cycleNum,state,payLoad.toString(), time));

//                    Handler for Activities
                    if (nodeID.equals("A") && state.equals("started"))
                    {
                        handleActivityA(sender, pmID, caseID, nodeID, cycleNum, payLoad);

                    }
                    if (nodeID.equals("B") && state.equals("started"))
                    {
                        handleActivityB(sender, pmID, caseID, nodeID, cycleNum, payLoad);

                    }
                });

                EPStatement statement2 = runtime.getDeploymentService().getStatement(dep.getDeploymentId(), "Execution-History");


                statement2.addListener((newData, oldData, stat, runt) -> {

                    for (int last =0; last < newData.length;last++) {
                        long pmID = (long) newData[last].get("pmID");
                        //      double x = (double) newData[0].get("x");
//                        int caseID = (int) newData[last].get("caseID");
//                        String nodeID = (String) newData[last].get("nodeID");
//                        int cycleNum = (int) newData[last].get("cycleNum");
//                        String state = (String) newData[last].get("state");
//                        Map<String, Object> payLoad = (Map<String, Object>) newData[last].get("payLoad");
//                        long time = (long) newData[last].get("timestamp");
//                        System.out.println(String.format("A new hhhhhhh event received with Process Model ID:%d," +
//                                        " Case ID:%d, Node ID:%s, Cycle Number:%d, State:%s, Payload:%s, and Time:%d",
//                                pmID, caseID, nodeID, cycleNum, state, payLoad.toString(), time));

                        System.out.println(String.format("Total events in the window is :%d",pmID));
                    }


                });

                Map<String, Object> variables = new HashMap<>();
                variables.put("cond1", Boolean.TRUE);
                variables.put("cond2", Boolean.FALSE);
                variables.put("cond3", Boolean.TRUE);
                variables.put("cond4", Boolean.FALSE);
                ProcessEvent startNewProcessInstance = new ProcessEvent(1,1, "SE1", 0, "started"
                        ,variables,System.currentTimeMillis());

                sender.sendEvent(startNewProcessInstance);

            } catch (IOException | ParseException | EPCompileException | EPDeployException | EPException e) {
                e.printStackTrace();
            }
        }


    }
//    private static boolean activityAHandled=false;
    private static void handleActivityA(EventSender sender, int pmID, int caseID, String nodeID, int cycleNum, Map<String, Object> payLoad) {
//        if (activityAHandled)
//            return;
        Map<String, Object> variables = new HashMap<>();

        Double v =Math.random();
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
        try {
            Thread.sleep((long) (v*10000));
            ProcessEvent activityACompleted = new ProcessEvent(pmID, caseID, nodeID, cycleNum,"completed",payLoad,System.currentTimeMillis());
            sender.sendEvent(activityACompleted);
        } catch (Exception e) {
            e.printStackTrace();
        }

//        activityAHandled=true;
    }

    private static void handleActivityB(EventSender sender, int pmID, int caseID, String nodeID, int cycleNum, Map<String, Object> payLoad) {
//        if (activityAHandled)
//            return;
        Map<String, Object> variables = new HashMap<>();

        Double v =Math.random();
        if ( v < 0.5) {
            variables.put("cond3", Boolean.TRUE);


        }
        else
        {
            variables.put("cond3", Boolean.FALSE);

        }
        try {
            Thread.sleep((long) (v*10000));
            ProcessEvent activityACompleted = new ProcessEvent(pmID, caseID, nodeID, cycleNum,"completed",payLoad,System.currentTimeMillis());
            sender.sendEvent(activityACompleted);
        } catch (Exception e) {
            e.printStackTrace();
        }

//        activityAHandled=true;
    }
}
