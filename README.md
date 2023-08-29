# Declarative Orchestration for Processes: CQL, Streams, and Tables Are All That You Need

This repository contains the implementation of our approach to utilize data stream management systems to serve as business process orchestrators. The implementation is dedicated to BPMN and Esper as concrete business process modeling languages and data stream management systems, respectively.

## Idea

Business processes are at the core of successful digital transformation initiatives. Process mining has proven valuable as a data-driven process analytics and enhancement approach. Through process mining, process models can be discovered, deviations between models and the actual execution can be identified, and improvements can be suggested. However, the utmost value of these insights will not materialize unless there is a solid and flexible process execution (enactment) infrastructure that can reflect the changes in running or future process instances. 

The process execution infrastructure consists of several information systems, external services, and human performers interacting to complete a business process instance. A process execution engine usually orchestrates the interaction and the order of execution of the different activities of a business process. There have been several technologies and solutions for process enactment and orchestration from academia and industry. YAWL is a prominent open-source workflow engine. BPEL is a standard for enacting business processes using web services. BPMN is a more recent and widely accepted standard for modeling and enacting business processes that are supported by several engines like Camunda [^1], Bizagi[^2], Activiti[^3], and many more. As these engines vary in their support for the execution semantics of the different modeling constructs, migrating running instances to a changed process model or deploying newer versions of the orchestrator has considerable technical debt and might result in vendor lock-in.

This work introduces a revolutionary approach to enacting and orchestrating business processes. We argue that using streams, tables, and the continuous query language (CQL), we can implement the execution semantics of several business process notations. 

In particular, this repository showcases implementing BPMN processes using the Esper data stream management system.

## Architecture

![Architecture](https://github.com/AhmedAwad/Flexible-Process-Enactment-CEP/blob/main/src/main/resources/Architecture1.jpg)
## Running the code

You can create BPMN models in any of the compatible editors that generate a .bpmn file a compliant XML format with the standard. To generate the CQL statements for the different constructs in the BPMN model, you can run the [RuleGenerator.generateEPLModule](https://github.com/AhmedAwad/Flexible-Process-Enactment-CEP/blob/44756f67b855aeddd7a9dd5d9e5f93e90ebfcedb/src/main/java/ee/ut/dsg/process/encatment/cep/RulesGenerator.java#L266). This will result in generating an .epl file that contains the CQL statements.

To start executing process instances based on the .epl file,  you can run the [Runner.main](https://github.com/AhmedAwad/Flexible-Process-Enactment-CEP/blob/44756f67b855aeddd7a9dd5d9e5f93e90ebfcedb/src/main/java/ee/ut/dsg/process/encatment/cep/Runner.java#L27) after pointing to the proper epl file.

[^1]:https://camunda.com/
[^2]:https://www.bizagi.com/
[^3]:https://www.activiti.org/ 
