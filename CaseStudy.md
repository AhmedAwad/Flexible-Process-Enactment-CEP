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






[^1]: [Declarative and Hybrid Process Discovery: Recent Advances and Open Challenges](https://link.springer.com/article/10.1007/s13740-020-00112-9)
