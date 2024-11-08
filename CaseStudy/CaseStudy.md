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

## DCR

<figure>
  <img src="./DCR%20Case%20Management-paper.png" alt="Alt Text" style="display: block; margin: 0 auto;">
  <figcaption style="text-align: center;">Figure 3: DCR model for the case management process</figcaption>
</figure>

We illustrate the corresponding model in a declarative process model using DCR graphs in Figure 3. The model is less complex, with no duplicate or artificial tasks. However, it is not easy to capture the starting and ending of the case. It takes a while to figure out that you can only execute ``Create Case`` at the beginning because it is set as a condition for all other events in the model. However, nothing prevents executing ``Create case`` several times for the same process instance (case). This can be remedied by adding an _exclude_ relation to itself. The same happens for the ``Close case`` and ``Lock case``. Therefore, there are no clear termination conditions for such a model, as is the case for DCR graphs in general.

## The Hybrid Model

<figure>
  <img src="./Hybrid%20Case%20Management%20example%20fro%20Tijs%20Paper.jpg" alt="Alt Text" style="display: block; margin: 0 auto;">
  <figcaption style="text-align: center;">Figure 4: Hybrid model for case management</figcaption>
</figure>


Figure 4 assumes a nested approach for modeling hybrid process models. The model uses procedural processes at the top level, in this case, a BPMN process model, and DCR graphs for the declarative part. The two steps of creating and closing the case take place in the beginning and the end, respectively. The ad-hoc sub-process will host the declarative part. We can observe that overall, the new model is simpler and has fewer artificial tasks. We only introduce the ``Finish case work`` task to explicitly define termination conditions for the ad-hoc sub-process.

## Generated CQL Statements

In the following, we show the mapping of the models in Figures 2, 3, and 4 into CQL respectively. 

### BPMN

The following statements are the mapping of the BPMN process in Figure 2 to CQL. The ``evaluate`` function, Line 12 to 58 is built to evaluate a condition ``cond`` against the case variables. The ``cond`` expression is instantiated by sequence flow conditions, see Line 77 where the function evaluates the condition ``nextAction != close``. In this example, we have used simple equal and not equal Boolean conditions. However, any complex Boolean condition can be used where the corresponding expression tree is constructed and referenced case variables are substituted with their variables from the ``Case_Variable`` table.

As an arriving ``ProcessEvent`` can trigger more than one CQL statement. It is necessary to force an execution order where it matters. The directive ``@Priority(n)`` is used by ESPER to give priorities to the CQL statements, the lower the number ``n``, the higher the priority. For instance, at Line 63, we give the priorty 1 to the CQL statement that updates the ``Execution_State`` table. This ensures that the event will upsert the table before  other rules need to process that event. All CQL statements that update the ``Case_Variables`` table have a higher priority, ``n=2``. Examples of such statements are in lines $167$ and $224$.



[View SQL Code with Line Numbers](https://gist.github.com/AhmedAwad/730ac1bbc133d412a7bb86759930d053)
```sql
--create a context
create context partitionedByPmIDAndCaseID partition by pmID, caseID from ProcessEvent;

@Audit
@name('track-events') context partitionedByPmIDAndCaseID select  pmID, caseID, nodeID, state, payLoad, timestamp  from ProcessEvent;

--Create the table that holds case variables
context partitionedByPmIDAndCaseID create table Case_Variables (pmID int primary key, caseID int primary key, variables java.util.Map);
@name('track-case-variables') context partitionedByPmIDAndCaseID select  pmID, caseID, variables from Case_Variables;

context partitionedByPmIDAndCaseID
create expression boolean js:evaluate(caseVariables, cond) [
    evaluate(caseVariables, cond);
    function evaluate(caseVariables, cond){
        if (cond == "true")
        {
            return true;
        }
 if (cond == "nextAction=upload")
                {
return caseVariables.get('nextAction')=='upload'
                 }
 if (cond == "nextAction=search")
                {
return caseVariables.get('nextAction')=='search'
                 }
 if (cond == "nextAction=download")
                {
return caseVariables.get('nextAction')=='download'
                 }
 if (cond == "nextAction=schedule")
                {
return caseVariables.get('nextAction')=='schedule'
                 }
 if (cond == "nextAction=hold")
                {
return caseVariables.get('nextAction')=='hold'
                 }
 if (cond == "nextAction=close")
                {
return caseVariables.get('nextAction')=='close'
                 }
 if (cond == "nextAction=lock")
                {
return caseVariables.get('nextAction')=='lock'
                 }
 if (cond == "nextAction=close")
                {
return caseVariables.get('nextAction')=='close'
                 }
 if (cond == "nextAction != close")
                {
return caseVariables.get('nextAction')!='close'
                 }

        return false;
    }
];
-- create the state table
context partitionedByPmIDAndCaseID create table Execution_State (pmID int primary key, caseID int primary key, nodeID string primary key, state string, timestamp long);@Audit
@name('track-state-table') context partitionedByPmIDAndCaseID select  pmID, caseID, nodeID, state, timestamp  from Execution_State;
--Update the state table on the occurrence of an event
@Priority(1)
context partitionedByPmIDAndCaseID on ProcessEvent as pe
merge Execution_State as es
where es.pmID = pe.pmID and es.caseID = pe.caseID and es.nodeID = pe.nodeID
when matched then
    update set es.state = pe.state, es.timestamp = pe.timestamp
when not matched then
    insert into Execution_State(pmID, caseID, nodeID, state, timestamp) select pe.pmID, pe.caseID, pe.nodeID,  pe.state, pe.timestamp;
--------------------------------------END of the Update State table--------------------------------------------
@Name('XOR-Join-loop-XJ-1-input-XS-2') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID,  state, payLoad, timestamp)
select pred.pmID, pred.caseID, "XJ-1",  pred.state,
 CV.variables, pred.timestamp
from ProcessEvent (state in ("completed") , nodeID = "XS-2") as pred join Case_Variables as CV
on pred.pmID = CV.pmID and pred.caseID = CV.caseID
where evaluate(CV.variables, "nextAction != close")=true;
-- XOR-join, when one of the inputs is forming a loop
-- The loopless entry point
@Name('XOR-Join-XJ-1') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID,  state, payLoad, timestamp)
select pred.pmID, pred.caseID, "XJ-1",  case pred.state when "completed" then "completed" else "skipped" end,
 CV.variables, pred.timestamp
from ProcessEvent (state in ("completed","skipped") , nodeID in ("Upload document")) as pred join Case_Variables as CV
on pred.pmID = CV.pmID and pred.caseID = CV.caseID;
-- XOR Split XS-1
-- Template to handle activity nodes that have a single predecessor
@Name('XOR-Split-XS-1') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID,  state, payLoad, Time_stamp)
select pred.pmID, pred.caseID, "XS-1", 
case when pred.state="completed" and  evaluate(CV.variables, "true") = true then "completed" else "skipped" end,
CV.variables, pred.timestamp
from ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID
where pred.state in ("completed", "skipped") and pred.nodeID in ("Decide what to do next");
@Name('XOR-Join-loop-XJ-2-input-XS-1') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID,  state, payLoad, timestamp)
select pred.pmID, pred.caseID, "XJ-2",  pred.state,
 CV.variables, pred.timestamp
from ProcessEvent (state in ("completed") , nodeID = "XS-1") as pred join Case_Variables as CV
on pred.pmID = CV.pmID and pred.caseID = CV.caseID
where evaluate(CV.variables, "nextAction=close")=true;
-- XOR-join, when one of the inputs is forming a loop
-- The loopless entry point
@Name('XOR-Join-XJ-2') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID,  state, payLoad, timestamp)
select pred.pmID, pred.caseID, "XJ-2",  case pred.state when "completed" then "completed" else "skipped" end,
 CV.variables, pred.timestamp
from ProcessEvent (state in ("completed","skipped") , nodeID in ("Upload document2","Search document","download oducment","Schedule meeting","Hold meeting","Lock case")) as pred join Case_Variables as CV
on pred.pmID = CV.pmID and pred.caseID = CV.caseID;
-- XOR Split XS-2
-- Template to handle activity nodes that have a single predecessor
@Name('XOR-Split-XS-2') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID,  state, payLoad, Time_stamp)
select pred.pmID, pred.caseID, "XS-2", 
case when pred.state="completed" and  evaluate(CV.variables, "true") = true then "completed" else "skipped" end,
CV.variables, pred.timestamp
from ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID
where pred.state in ("completed", "skipped") and pred.nodeID in ("XJ-2");
-- Activity Create Case-Skipped
-- Template to handle activity nodes that have a single predecessor
@Priority(1) @Name('Activity-Create Case-Skip') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID,  state, payLoad, Time_stamp)
select pred.pmID, pred.caseID, "Create Case","skipped",CV.variables, pred.timestamp
From ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID
where (pred.state = "skipped" or evaluate(CV.variables,"true")=false) and pred.nodeID in ("SE");
-- Activity Create Case-Completed
-- Template to handle activity nodes that have a single predecessor
@Name('Activity-Create Case-Start') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID,  state, payLoad, Time_stamp)
select pred.pmID, pred.caseID, "Create Case","started",CV.variables, pred.timestamp
From ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID
where pred.state = "completed" and evaluate(CV.variables,"true")=true and pred.nodeID in ("SE");

-- Activity Close case-Skipped
-- Template to handle activity nodes that have a single predecessor
@Priority(1) @Name('Activity-Close case-Skip') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID,  state, payLoad, Time_stamp)
select pred.pmID, pred.caseID, "Close case","skipped",CV.variables, pred.timestamp
From ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID
where (pred.state = "skipped" or evaluate(CV.variables,"nextAction=close")=false) and pred.nodeID in ("XS-2");
-- Activity Close case-Completed
-- Template to handle activity nodes that have a single predecessor
@Name('Activity-Close case-Start') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID,  state, payLoad, Time_stamp)
select pred.pmID, pred.caseID, "Close case","started",CV.variables, pred.timestamp
From ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID
where pred.state = "completed" and evaluate(CV.variables,"nextAction=close")=true and pred.nodeID in ("XS-2");

-- Activity Upload document-Skipped
-- Template to handle activity nodes that have a single predecessor
@Priority(1) @Name('Activity-Upload document-Skip') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID,  state, payLoad, Time_stamp)
select pred.pmID, pred.caseID, "Upload document","skipped",CV.variables, pred.timestamp
From ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID
where (pred.state = "skipped" or evaluate(CV.variables,"true")=false) and pred.nodeID in ("Create Case");
-- Activity Upload document-Completed
-- Template to handle activity nodes that have a single predecessor
@Name('Activity-Upload document-Start') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID,  state, payLoad, Time_stamp)
select pred.pmID, pred.caseID, "Upload document","started",CV.variables, pred.timestamp
From ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID
where pred.state = "completed" and evaluate(CV.variables,"true")=true and pred.nodeID in ("Create Case");

-- Activity Decide what to do next-Skipped
-- Template to handle activity nodes that have a single predecessor
@Priority(1) @Name('Activity-Decide what to do next-Skip') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID,  state, payLoad, Time_stamp)
select pred.pmID, pred.caseID, "Decide what to do next","skipped",CV.variables, pred.timestamp
From ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID
where (pred.state = "skipped" or evaluate(CV.variables,"true")=false) and pred.nodeID in ("XJ-1");
-- Activity Decide what to do next-Completed
-- Template to handle activity nodes that have a single predecessor
@Name('Activity-Decide what to do next-Start') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID,  state, payLoad, Time_stamp)
select pred.pmID, pred.caseID, "Decide what to do next","started",CV.variables, pred.timestamp
From ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID
where pred.state = "completed" and evaluate(CV.variables,"true")=true and pred.nodeID in ("XJ-1");

--Update case variable on the completion of activity Decide what to do next
@Priority(2) context partitionedByPmIDAndCaseID 
on ProcessEvent(nodeID="Decide what to do next", state="completed") as a
update Case_Variables as CV set variables('nextAction') = a.payLoad('nextAction')
where CV.pmID = a.pmID and CV.caseID = a.caseID;
-- Activity Search document-Skipped
-- Template to handle activity nodes that have a single predecessor
@Priority(1) @Name('Activity-Search document-Skip') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID,  state, payLoad, Time_stamp)
select pred.pmID, pred.caseID, "Search document","skipped",CV.variables, pred.timestamp
From ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID
where (pred.state = "skipped" or evaluate(CV.variables,"nextAction=search")=false) and pred.nodeID in ("XS-1");
-- Activity Search document-Completed
-- Template to handle activity nodes that have a single predecessor
@Name('Activity-Search document-Start') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID,  state, payLoad, Time_stamp)
select pred.pmID, pred.caseID, "Search document","started",CV.variables, pred.timestamp
From ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID
where pred.state = "completed" and evaluate(CV.variables,"nextAction=search")=true and pred.nodeID in ("XS-1");

-- Activity download oducment-Skipped
-- Template to handle activity nodes that have a single predecessor
@Priority(1) @Name('Activity-download oducment-Skip') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID,  state, payLoad, Time_stamp)
select pred.pmID, pred.caseID, "download oducment","skipped",CV.variables, pred.timestamp
From ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID
where (pred.state = "skipped" or evaluate(CV.variables,"nextAction=download")=false) and pred.nodeID in ("XS-1");
-- Activity download oducment-Completed
-- Template to handle activity nodes that have a single predecessor
@Name('Activity-download oducment-Start') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID,  state, payLoad, Time_stamp)
select pred.pmID, pred.caseID, "download oducment","started",CV.variables, pred.timestamp
From ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID
where pred.state = "completed" and evaluate(CV.variables,"nextAction=download")=true and pred.nodeID in ("XS-1");

-- Activity Upload document2-Skipped
-- Template to handle activity nodes that have a single predecessor
@Priority(1) @Name('Activity-Upload document2-Skip') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID,  state, payLoad, Time_stamp)
select pred.pmID, pred.caseID, "Upload document2","skipped",CV.variables, pred.timestamp
From ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID
where (pred.state = "skipped" or evaluate(CV.variables,"nextAction=upload")=false) and pred.nodeID in ("XS-1");
-- Activity Upload document2-Completed
-- Template to handle activity nodes that have a single predecessor
@Name('Activity-Upload document2-Start') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID,  state, payLoad, Time_stamp)
select pred.pmID, pred.caseID, "Upload document2","started",CV.variables, pred.timestamp
From ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID
where pred.state = "completed" and evaluate(CV.variables,"nextAction=upload")=true and pred.nodeID in ("XS-1");

-- Activity Schedule meeting-Skipped
-- Template to handle activity nodes that have a single predecessor
@Priority(1) @Name('Activity-Schedule meeting-Skip') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID,  state, payLoad, Time_stamp)
select pred.pmID, pred.caseID, "Schedule meeting","skipped",CV.variables, pred.timestamp
From ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID
where (pred.state = "skipped" or evaluate(CV.variables,"nextAction=schedule")=false) and pred.nodeID in ("XS-1");
-- Activity Schedule meeting-Completed
-- Template to handle activity nodes that have a single predecessor
@Name('Activity-Schedule meeting-Start') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID,  state, payLoad, Time_stamp)
select pred.pmID, pred.caseID, "Schedule meeting","started",CV.variables, pred.timestamp
From ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID
where pred.state = "completed" and evaluate(CV.variables,"nextAction=schedule")=true and pred.nodeID in ("XS-1");

--Update case variable on the completion of activity Schedule meeting
@Priority(2) context partitionedByPmIDAndCaseID 
on ProcessEvent(nodeID="Schedule meeting", state="completed") as a
update Case_Variables as CV set variables('holdMeeting') = a.payLoad('holdMeeting')
where CV.pmID = a.pmID and CV.caseID = a.caseID;
-- Activity Hold meeting-Skipped
-- Template to handle activity nodes that have a single predecessor
@Priority(1) @Name('Activity-Hold meeting-Skip') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID,  state, payLoad, Time_stamp)
select pred.pmID, pred.caseID, "Hold meeting","skipped",CV.variables, pred.timestamp
From ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID
where (pred.state = "skipped" or evaluate(CV.variables,"nextAction=hold")=false) and pred.nodeID in ("XS-1");
-- Activity Hold meeting-Completed
-- Template to handle activity nodes that have a single predecessor
@Name('Activity-Hold meeting-Start') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID,  state, payLoad, Time_stamp)
select pred.pmID, pred.caseID, "Hold meeting","started",CV.variables, pred.timestamp
From ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID
where pred.state = "completed" and evaluate(CV.variables,"nextAction=hold")=true and pred.nodeID in ("XS-1");

--Update case variable on the completion of activity Hold meeting
@Priority(2) context partitionedByPmIDAndCaseID 
on ProcessEvent(nodeID="Hold meeting", state="completed") as a
update Case_Variables as CV set variables('holdMeeting') = a.payLoad('holdMeeting')
where CV.pmID = a.pmID and CV.caseID = a.caseID;
-- Activity Lock case-Skipped
-- Template to handle activity nodes that have a single predecessor
@Priority(1) @Name('Activity-Lock case-Skip') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID,  state, payLoad, Time_stamp)
select pred.pmID, pred.caseID, "Lock case","skipped",CV.variables, pred.timestamp
From ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID
where (pred.state = "skipped" or evaluate(CV.variables,"nextAction=lock")=false) and pred.nodeID in ("XS-1");
-- Activity Lock case-Completed
-- Template to handle activity nodes that have a single predecessor
@Name('Activity-Lock case-Start') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID,  state, payLoad, Time_stamp)
select pred.pmID, pred.caseID, "Lock case","started",CV.variables, pred.timestamp
From ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID
where pred.state = "completed" and evaluate(CV.variables,"nextAction=lock")=true and pred.nodeID in ("XS-1");

--Update case variable on the completion of activity Lock case
@Priority(2) context partitionedByPmIDAndCaseID 
on ProcessEvent(nodeID="Lock case", state="completed") as a
update Case_Variables as CV set variables('caseLocked') = a.payLoad('caseLocked')
where CV.pmID = a.pmID and CV.caseID = a.caseID;
-- End event-EE-skip
@Priority(1) @Name('End-Event-skip') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID,  state, payLoad, timestamp)
select pred.pmID, pred.caseID, "EE","skipped",CV.variables, pred.timestamp
From ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID
where (pred.state in ("skipped") or evaluate(CV.variables, "true") = false ) and pred.nodeID in ("Close case");
-- End event-EE-complete
@Name('End-Event-complete') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID,  state, payLoad, timestamp)
select pred.pmID, pred.caseID, "EE","completed",CV.variables, pred.timestamp
From ProcessEvent as pred join Case_Variables as CV on pred.pmID = CV.pmID and pred.caseID = CV.caseID
where pred.state in ("completed") and evaluate(CV.variables, "true") = true and pred.nodeID in ("Close case");

@Priority(5) context partitionedByPmIDAndCaseID on ProcessEvent(nodeID="EE", state="completed") as a
delete from Execution_State as H
where H.pmID = a.pmID and H.caseID = a.caseID
and not exists (select 1 from Execution_State as H where H.pmID = a.pmID and H.caseID = a.caseID and
H.nodeID = "EE" and H.state ="completed");

-- Start event -- this shall be injected from outside
@Name('Start-Event-SE') context partitionedByPmIDAndCaseID insert into ProcessEvent(pmID, caseID, nodeID,  state, payLoad, timestamp)
select pred.pmID, pred.caseID, "SE","completed", pred.payLoad, pred.timestamp
from ProcessEvent(nodeID="SE", state="started") as pred;
--Inititate case variables as a response to the start event
@Name('Insert-Case-Variables') context partitionedByPmIDAndCaseID
insert into Case_Variables (pmID, caseID, variables )
select st.pmID, st.caseID, st.payLoad from ProcessEvent(nodeID="SE", state="started") as st;
```
### DCR

The following code snippet shows the mapping of the DCR process in Figure 3 to CQL. We start by creating the ``EventState`` table. The CQL statement from line $3$ to $8$ reports the eligible tasks for execution upon the arrival of a process execution event.  This statement is given a lower priority than the statement at line $13$. The latter updates the $EventState$ table to reflect the execution of a task by setting ``happened`` to true and ``restless`` to false only if the task was eligible for execution, i.e., ``included=true``. The whole instance is triggered by an external event. This is given in lines $18$ to $33$ which initializes the ``EventState`` table for all tasks according to the model. As none of the tasks would have been executed at this stage, they all have their ``happened`` property set to ``false``. Moreover, as ``Search Document`` is initially excluded by the process model design, its ``included`` property is set to ``false``. The model rules are realized by CQL statements from Line $35$ onwards

```sql
@Priority(2) @Name('available-tasks') on ProcessEvent as a
Select ProcessModelID, ES.caseID as caseID, eventID from EventState as ES
Where included=true and ES.ProcessModelID = a.pmID and ES.caseID = a.caseID
and ((ES.eventID ="Close Case" and exists (Select 1 from EventState as ES2 Where ES2.eventID = "Create Case" and ES2.caseID = ES.caseID 
and (not ES2.included or (ES.included and ES2.happened)) )) 
or ES.eventID in ("Create Case", "Download document", "Lock case", "Search documents", "Schedule Meeting", "Upload document", "Hold Meeting"));

@Priority(20)Select  pmID, caseID, nodeID, state, payLoad, timestamp  from ProcessEvent;

-- Update an activity to be happened (executed) and no longer required (restless=false) */
@Priority(10)  on ProcessEvent as a
Update EventState as ES set restless = false, happened=true
Where ES.included = true and ES.ProcessModelID = a.pmID and ES.caseID = a.caseID and ES.eventID=a.nodeID;

-- Initiate the state table upon an external trigger for the new case */
@Priority(5)  on ProcessEvent(nodeID="SE", state="started") as a  Insert into EventState(ProcessModelID,caseID,eventID,happened,included,restless)
Select a.pmID,a.caseID,"Hold Meeting",false,false,false;
@Priority(5)  on ProcessEvent(nodeID="SE", state="started") as a  Insert into EventState(ProcessModelID,caseID,eventID,happened,included,restless)
Select a.pmID,a.caseID,"Close Case",false,true,false;
@Priority(5)  on ProcessEvent(nodeID="SE", state="started") as a  Insert into EventState(ProcessModelID,caseID,eventID,happened,included,restless)
Select a.pmID,a.caseID,"Download document",false,false,false;
@Priority(5)  on ProcessEvent(nodeID="SE", state="started") as a  Insert into EventState(ProcessModelID,caseID,eventID,happened,included,restless)
Select a.pmID,a.caseID,"Lock case",false,true,false;
@Priority(5)  on ProcessEvent(nodeID="SE", state="started") as a  Insert into EventState(ProcessModelID,caseID,eventID,happened,included,restless)
Select a.pmID,a.caseID,"Search documents",false,false,false;
@Priority(5)  on ProcessEvent(nodeID="SE", state="started") as a  Insert into EventState(ProcessModelID,caseID,eventID,happened,included,restless)
Select a.pmID,a.caseID,"Schedule Meeting",false,true,false;
@Priority(5)  on ProcessEvent(nodeID="SE", state="started") as a  Insert into EventState(ProcessModelID,caseID,eventID,happened,included,restless)
Select a.pmID,a.caseID,"Create Case",false,true,false;
@Priority(5)  on ProcessEvent(nodeID="SE", state="started") as a  Insert into EventState(ProcessModelID,caseID,eventID,happened,included,restless)
Select a.pmID,a.caseID,"Upload document",false,true,false;

-- Precondition rule */
@Priority(10)  on ProcessEvent(nodeID="Close Case") as a
Update EventState as ES set restless = false, happened=true
Where ES.ProcessModelID = a.pmID and ES.caseID = a.caseID and ES.eventID="Close Case"
 and exists (Select 1 from EventState as ES2 Where ES2.eventID = "Create Case" and ES2.caseID = ES.caseID 
and (not ES2.included or (ES.included and ES2.happened)));


-- exclude(Close Case, Hold Meeting) */
@Priority(5)  on ProcessEvent(nodeID="Close Case") as a
Update EventState as ES set included = false
Where ES.ProcessModelID = a.pmID and ES.caseID = a.caseID and ES.eventID="Hold Meeting"
and exists (Select 1 from EventState as ES2 Where ES.ProcessModelID = a.pmID and ES2.caseID = a.caseID and ES2.eventID = a.nodeID and ES2.happened = true);

-- exclude(Close Case, Close Case) */
@Priority(5)  on ProcessEvent(nodeID="Close Case") as a
Update EventState as ES set included = false
Where ES.ProcessModelID = a.pmID and ES.caseID = a.caseID and ES.eventID="Close Case" 
and exists (Select 1 from EventState as ES2 Where ES.ProcessModelID = a.pmID and ES2.caseID = a.caseID and ES2.eventID = a.nodeID and ES2.happened = true);

-- exclude(Schedule Meeting, Schedule Meeting) */
@Priority(5)  on ProcessEvent(nodeID="Schedule Meeting") as a
Update EventState as ES set included = false
Where ES.ProcessModelID = a.pmID and ES.caseID = a.caseID and ES.eventID="Schedule Meeting" 
and exists (Select 1 from EventState as ES2 Where ES.ProcessModelID = a.pmID and ES2.caseID = a.caseID and ES2.eventID = a.nodeID and ES2.happened = true);

-- exclude(Create Case, Create Case) */
@Priority(5)  on ProcessEvent(nodeID="Create Case") as a
Update EventState as ES set included = false
Where ES.ProcessModelID = a.pmID and ES.caseID = a.caseID and ES.eventID="Create Case"
and exists (Select 1 from EventState as ES2 Where ES.ProcessModelID = a.pmID and ES2.caseID = a.caseID and ES2.eventID = a.nodeID and ES2.happened = true);

-- response(Close Case,Create Case) */
@Priority(5)  on ProcessEvent(nodeID="Close Case") as a
Update EventState as ES set restless = true
Where ES.ProcessModelID = a.pmID and ES.caseID = a.caseID and ES.eventID="Create Case"
and exists (Select 1 from EventState as ES2 Where ES.ProcessModelID = a.pmID and ES2.caseID = a.caseID and ES2.eventID = a.nodeID and ES2.happened = true);

-- exclude(Close Case, Schedule Meeting) */
@Priority(5)  on ProcessEvent(nodeID="Close Case") as a
Update EventState as ES set included = false
Where ES.ProcessModelID = a.pmID and ES.caseID = a.caseID and ES.eventID="Schedule Meeting"
and exists (Select 1 from EventState as ES2 Where ES.ProcessModelID = a.pmID and ES2.caseID = a.caseID and ES2.eventID = a.nodeID and ES2.happened = true);

-- exclude(Close Case, Lock case) */
@Priority(5)  on ProcessEvent(nodeID="Close Case") as a
Update EventState as ES set included = false
Where ES.ProcessModelID = a.pmID and ES.caseID = a.caseID and ES.eventID="Lock case"
and exists (Select 1 from EventState as ES2 Where ES.ProcessModelID = a.pmID and ES2.caseID = a.caseID and ES2.eventID = a.nodeID and ES2.happened = true);

-- exclude(Lock case, Upload document) */
@Priority(5)  on ProcessEvent(nodeID="Lock case") as a
Update EventState as ES set included = false
Where ES.ProcessModelID = a.pmID and ES.caseID = a.caseID and ES.eventID="Upload document"
and exists (Select 1 from EventState as ES2 Where ES.ProcessModelID = a.pmID and ES2.caseID = a.caseID and ES2.eventID = a.nodeID and ES2.happened = true);

-- exclude(Close Case, Upload document) */
@Priority(5)  on ProcessEvent(nodeID="Close Case") as a
Update EventState as ES set included = false
Where ES.ProcessModelID = a.pmID and ES.caseID = a.caseID and ES.eventID="Upload document"
and exists (Select 1 from EventState as ES2 Where ES.ProcessModelID = a.pmID and ES2.caseID = a.caseID and ES2.eventID = a.nodeID and ES2.happened = true);

-- include(Upload document, Download document) */
@Priority(5)  on ProcessEvent(nodeID="Upload document") as a
Update EventState as ES set included = true
Where ES.ProcessModelID = a.pmID and ES.caseID = a.caseID and ES.eventID="Download document"
and exists (Select 1 from EventState as ES2 Where ES.ProcessModelID = a.pmID and ES2.caseID = a.caseID and ES2.eventID = a.nodeID and ES2.happened = true);

-- include(Upload document, Search documents) */ 
@Priority(5)  on ProcessEvent(nodeID="Upload document") as a
Update EventState as ES set included = true
Where ES.ProcessModelID = a.pmID and ES.caseID = a.caseID and ES.eventID="Search documents"
and exists (Select 1 from EventState as ES2 Where ES.ProcessModelID = a.pmID and ES2.caseID = a.caseID and ES2.eventID = a.nodeID and ES2.happened = true);

-- exclude(Hold Meeting, Hold Meeting) */
@Priority(5)  on ProcessEvent(nodeID="Hold Meeting") as a
Update EventState as ES set included = false
Where ES.ProcessModelID = a.pmID and ES.caseID = a.caseID and ES.eventID="Hold Meeting"
and exists (Select 1 from EventState as ES2 Where ES.ProcessModelID = a.pmID and ES2.caseID = a.caseID and ES2.eventID = a.nodeID and ES2.happened = true);

-- include(Hold Meeting, Schedule Meeting) */
@Priority(5)  on ProcessEvent(nodeID="Hold Meeting") as a
Update EventState as ES set included = true
Where ES.ProcessModelID = a.pmID and ES.caseID = a.caseID and ES.eventID="Schedule Meeting"
and exists (Select 1 from EventState as ES2 Where ES.ProcessModelID = a.pmID and ES2.caseID = a.caseID and ES2.eventID = a.nodeID and ES2.happened = true);

-- include(Schedule Meeting, Hold Meeting) */
@Priority(5)  on ProcessEvent(nodeID="Schedule Meeting") as a
Update EventState as ES set included = true
Where ES.ProcessModelID = a.pmID and ES.caseID = a.caseID and ES.eventID="Hold Meeting"
and exists (Select 1 from EventState as ES2 Where ES.ProcessModelID = a.pmID and ES2.caseID = a.caseID and ES2.eventID = a.nodeID and ES2.happened = true);

-- exclude(Close Case, Download document) */
@Priority(5)  on ProcessEvent(nodeID="Close Case") as a
Update EventState as ES set included = false
Where ES.ProcessModelID = a.pmID and ES.caseID = a.caseID and ES.eventID="Download document"
and exists (Select 1 from EventState as ES2 Where ES.ProcessModelID = a.pmID and ES2.caseID = a.caseID and ES2.eventID = a.nodeID and ES2.happened = true);

-- exclude(Close Case, Search documents) */
@Priority(5)  on ProcessEvent(nodeID="Close Case") as a
Update EventState as ES set included = false
Where ES.ProcessModelID = a.pmID and ES.caseID = a.caseID and ES.eventID="Search documents"
and exists (Select 1 from EventState as ES2 Where ES.ProcessModelID = a.pmID and ES2.caseID = a.caseID and ES2.eventID = a.nodeID and ES2.happened = true);
```

[^1]: [Declarative and Hybrid Process Discovery: Recent Advances and Open Challenges](https://link.springer.com/article/10.1007/s13740-020-00112-9)
[^2]: Camunda does not support the execution of Ad-hoc sub-processes.
