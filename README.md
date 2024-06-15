# A Flexible and Unified Approach for Process Enactment: Streams and Tables are All You Need

This repository contains the implementation of our approach to utilize data stream management systems to serve as business process orchestrators. The implementation is dedicated to BPMN, as an imperative process modeling language, DECR graphs as a decalarative process modeling language, and Esper as a stream management system.

## Idea

Business processes are at the core of successful digital transformation initiatives. Process mining has proven valuable as a data-driven process analytics and enhancement approach. Through process mining, process models can be discovered, deviations between models and the actual execution can be identified, and improvements can be suggested. However, the utmost value of these insights will not materialize unless there is a solid and flexible process execution (enactment) infrastructure that can reflect the changes in running or future process instances. 

At the process models' representation level, two mindsets prevail: procedural (so-called) imperative notations describe the flows in a process, while declarative notations describe the the process as set of constraints. While each has its own benefits in terms of representation and cognitive-effectiveness, they rarely combine, and there is lack of compatibility between notations. The utmost benefit is achieved with mixed models. 

The process execution infrastructure consists of several information systems, external services, and human performers interacting to complete a business process instance. A process execution engine usually orchestrates the interaction and the order of execution of the different activities of a business process. There have been several technologies and solutions for process enactment and orchestration from academia and industry. YAWL~\cite{YAWL} is a prominent open-source workflow engine. BPEL~\cite{BPEL} is a standard for enacting business processes using web services. BPMN~\cite{omg2014bpmn} is a more recent and widely accepted standard for modelling and enacting imperative business processes that are supported by several engines like Camunda[^1], Bizagi[^2], Activiti[^3], and many more. Declare models can be defined and executed over CPN Tools[^4] for declarative languages. This entails translating the declarative constraints into a procedural coloured Petri net form. DCR Graphs[^5] is a modeling and execution engine for dynamic conditions response (DCR). As these engines vary in their support for the execution semantics of the different modelling constructs, migrating running instances to a changed process model or deploying newer versions of the orchestrator has considerable technical debt and might result in vendor lock-in. 


This work introduces a uniform approach to enacting and orchestrating business processes. We argue that using streams, tables, and the continuous query language (CQL)[^6], we can implement the execution semantics of several business process notations. Moreover, we can execute hybrid process models using the same orchestration infrastructure.

In particular, this repository showcases implementing  the execution of BPMN process models, as an imperative language, and DECR graphs, as a decalartive language, using the Esper data stream management system, as a faithful implementation of CQL.

You can find more technical details and examples on [these slides for BPMN](https://docs.google.com/presentation/d/1Fsy-CRxOXGYy1KB5Co-FxJG79Y5bLsNdQhWtQdc5p_4/edit?usp=sharing) and [these slides for DCR graphs](https://docs.google.com/presentation/d/1-ju54NfTdjj6qnBwIYa5YW_yIFcVGrnta8iXqyX7gNA/edit?usp=sharing)

## Architecture

![Architecture](https://github.com/AhmedAwad/Flexible-Process-Enactment-CEP/blob/main/src/main/resources/Architecture1.jpg)
## Running the code

You can create BPMN models in any of the compatible editors that generate a .bpmn file a compliant XML format with the standard. To generate the CQL statements for the different constructs in the BPMN model, you can run the [RuleGenerator.generateEPLModule](https://github.com/AhmedAwad/Flexible-Process-Enactment-CEP/blob/44756f67b855aeddd7a9dd5d9e5f93e90ebfcedb/src/main/java/ee/ut/dsg/process/encatment/cep/RulesGenerator.java#L266). This will result in generating an .epl file that contains the CQL statements.

To start executing process instances based on the .epl file,  you can run the [Runner.main](https://github.com/AhmedAwad/Flexible-Process-Enactment-CEP/blob/44756f67b855aeddd7a9dd5d9e5f93e90ebfcedb/src/main/java/ee/ut/dsg/process/encatment/cep/Runner.java#L27) after pointing to the proper epl file.

[^1]:https://camunda.com/
[^2]:https://www.bizagi.com/
[^3]:https://www.activiti.org/ 
[^4]:https://www.win.tue.nl/declare/
[^5]:https://www.dcrgraphs.net/
[^6]:http://infolab.stanford.edu/~arvind/papers/cql-vldbj.pdf
