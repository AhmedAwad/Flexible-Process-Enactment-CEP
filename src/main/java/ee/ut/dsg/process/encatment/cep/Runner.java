package ee.ut.dsg.process.encatment.cep;

import java.io.*;

import com.espertech.esper.common.client.EPCompiled;
import com.espertech.esper.common.client.configuration.Configuration;
import com.espertech.esper.common.client.module.Module;
import com.espertech.esper.common.client.module.ParseException;
import com.espertech.esper.common.client.util.EventTypeBusModifier;
import com.espertech.esper.compiler.client.CompilerArguments;
import com.espertech.esper.compiler.client.EPCompileException;
import com.espertech.esper.compiler.client.EPCompiler;
import com.espertech.esper.compiler.client.EPCompilerProvider;
import com.espertech.esper.runtime.client.*;
import ee.ut.dsg.process.encatment.cep.events.Process_Event;


public class Runner {

    public static  void main(String[] args)
    {
        EPCompiler compiler = EPCompilerProvider.getCompiler();

        Configuration configuration = new Configuration();
        configuration.getCommon().addEventType(Process_Event.class);

        configuration.getRuntime().getExecution().setPrioritized(true);
        configuration.getRuntime().getThreading().setInternalTimerEnabled(true);
        configuration.getCompiler().getByteCode().setBusModifierEventType(EventTypeBusModifier.BUS);
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



            } catch (IOException | ParseException | EPCompileException | EPDeployException e) {
                e.printStackTrace();
            }
        }


    }
}
