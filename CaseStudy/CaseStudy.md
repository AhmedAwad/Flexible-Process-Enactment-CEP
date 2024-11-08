# Case study: Executing procedural, declarative, and hybrid process models using out BEST approach 
## Description
We validate our approach by means mapping a process model from the literature into a set of CQL statements. 
We start by discussing the requirements for the process. Then, we develop a procedural BPMN process, a declarative DCR process, and a hybrid model combining imperative and declarative aspects, 
and show how they can be mapped to CQL and their execution semantics. Moreover, we compare the execution of our BPMN and DCR processes to those deployed on Camunda and DCRGraphs, 
respectively, to show that we can reach similar execution sequences. The comparisons were on selected execution scenarios and are not meant to show full equivalence, this is a subject for future work.

## Requirements for a case management process

We use the process description from the paper[^1]  of a  case management process with the following requirements:
- Every case of the case management system is initially created and eventually closed,
- For a created case, an arbitrary number of documents can be uploaded,
- An uploaded document can be downloaded or searched,
- At any time, a case can be locked, 
- After locking a case, it is not possible to upload a document; still, uploaded documents can be downloaded and searched, 
- Furthermore, in every case, meetings can be held. To hold a meeting, it has to be (re) scheduled, 
- Meetings can be rescheduled arbitrarily often, however, it is not possible to schedule more than one meeting in advance. 

The process has parts that can be best modeled following a procedural approach, i.e., the explicit start (creating a case) and end (closing a case), and another part that can be better captured by embracing a declarative approach, i.e., uploading, downloading documents, and scheduling and conducting meetings. In the following, we show how the process can be represented using BPMN and DCR graphs, and how it can benefit from using a hybrid approach. The objective is not to discuss the expressiveness of the modeling languages. Rather, we evaluate the flexibility of our approach for executing business processes.

## BPMN
<figure>
  <img src="./Case%20Management%20BPMN.png" alt="Alt Text">
  <figcaption style="text-align: center;">Figure 1: BPMN model for the case management process</figcaption>
</figure>


Figure 1 captures the above process requirements. The process model is adapted from[^1], with the following observations:

+ The upload document task is duplicated. The first one is to force a document to be uploaded before any further actions can be taken on the case. The second copy of the task is to allow the optional behavior of uploading other documents,
+ Two artificial tasks were introduced to make the process semantics clearer,
  + The task ``Choose what to do next`` is introduced in a looping behavior to give the process performer the chance to either upload another document, lock the case, or do nothing. If she chooses to upload a document or do nothing, she will be offered to execute the ``Choose`` task again. If the case is locked, the control flow token is passed to the AND-join and waits for the termination of the other parallel branch.
  + The task ``Prepare to close case`` is added to the ad-hoc sub-process to force a clear termination condition for the sub-process. Otherwise, one of the tasks among ``download document``, ``search document``, or ``schedule meeting`` can be executed.


This model exposes additional behavior that is not in the original description. Both tasks ``Choose what to do next`` and ``Prepare to close case`` are not genuinely parts of the requirements and do not contribute a business value. Additionally, notice that the BPMN process does not fully capture the constraints in the process description. The description states that it is not possible to schedule more than one meeting in advance. If we look at the execution semantics of the BPMN Ad-hoc subprocess, we can execute the ``Schedule Meeting`` task several times before we conduct the meeting.


<figure>
  <img src="./Case%20Management%20example%20fro%20Tijs%20Paper%20V2-1.png" alt="Alt Text">
  <figcaption style="text-align: center;">Figure 2: BPMN model for the case management process - simplified</figcaption>
</figure>


To stick to common BPMN elements that are truly procedural, we get rid of the Ad-hoc sub-process and remodel the process from Figure 1 as shown in Figure 2. We get rid of the artificial task ``Prepare to close case``. Yet, we lose the parallelism of uploading documents, searching document, planning and holding meetings. The simplification allows modeling this process in other procedural process modeling languages and for comparison with other BPMN-compliant execution engines, e.g., Camunda[^2].
The model in Figure 2 uses variables to control the execution flow. The logic of the ``Decide what to do next`` sets the value of the _nextAction_ variable. Moreover, the logic for forcing scheduling a meeting before holding it, blocking documents upload once the case is locked, and not scheduling more than one meeting are all decided within the ``Decide what to do next`` task.



[^1]: [Declarative and Hybrid Process Discovery: Recent Advances and Open Challenges](https://link.springer.com/article/10.1007/s13740-020-00112-9)
[^2]: Camunda does not support the execution of Ad-hoc sub-processes.
