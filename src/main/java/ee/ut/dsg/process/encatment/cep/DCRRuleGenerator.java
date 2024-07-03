package ee.ut.dsg.process.encatment.cep;

import ee.ut.dsg.process.encatment.cep.dcr.DCRGraph;
import ee.ut.dsg.process.encatment.cep.dcr.Event;
import ee.ut.dsg.process.encatment.cep.dcr.Relation;
import ee.ut.dsg.process.encatment.cep.dcr.RelationType;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import java.io.*;
import java.util.Iterator;
import java.util.Set;


public class DCRRuleGenerator extends RuleGenerator{

    private final long processModelID;
    private final long caseID;



    private static final String DEFINE_CONTEXT ="create context partitionedByPmIDAndCaseID partition by pmID, caseID from ProcessEvent;\n";




    private static final String EVENT_STATE_TABLE ="context partitionedByPmIDAndCaseID\n"+
    "create table EventState (ProcessModelID int primary key, caseID int primary key,\n"+
                             "eventID string primary key, happened boolean, included boolean, restless boolean);\n";

    private static  final String DEFINE_TRACK_EVENTS ="@Audit @Priority(2) @name('track-dcr-event') context partitionedByPmIDAndCaseID \n" +
            "on ProcessEvent as a\n" +
            "select ProcessModelID, ES.caseID as caseID, eventID from EventState as ES\n" +
            "where included=true and ES.ProcessModelID = a.pmID and ES.caseID = a.caseID;\n";
    private static final String INITIAL_EVENT_STATE = "@Audit @name('init-state-table') @Priority(5) context partitionedByPmIDAndCaseID on ProcessEvent(nodeID=\"SE\", state=\"started\") as a  insert into EventState(ProcessModelID,caseID,eventID,happened,included,restless)\n" +
            "select a.pmID,a.caseID,\"%s\",%s,%s,%s;\n";


    private static final String UPDATE_EVENT_STATE ="// Update an activity to be happened (executed) and no longer required (restless=false)\n" +
            "context partitionedByPmIDAndCaseID on ProcessEvent as a\n" +
            "update EventState as ES set restless = false, happened=true\n" +
            "where ES.included = true and ES.ProcessModelID = a.pmID and ES.caseID = a.caseID and ES.eventID=a.nodeID;\n";

    private static final String EXCLUDE_RULE ="// exclude(%s, %s)\n" +
            "@Priority(5) context partitionedByPmIDAndCaseID on ProcessEvent(nodeID=\"%s\") as a\n" +
            "update EventState as ES set included = false\n" +
            "where ES.ProcessModelID = a.pmID and ES.caseID = a.caseID and ES.eventID=\"%s\";\n";

    private static final String INCLUDE_RULE ="// include(%s, %s)\n" +
            "@Priority(5) context partitionedByPmIDAndCaseID on ProcessEvent(nodeID=\"%s\") as a\n" +
            "update EventState as ES set included = true\n" +
            "where ES.ProcessModelID = a.pmID and ES.caseID = a.caseID and ES.eventID=\"%s\";\n";

    private static final String RESPONSE_RULE ="// response(%s,%s)\n"+
            "@Priority(5) context partitionedByPmIDAndCaseID on ProcessEvent(nodeID=\"%s\") as a\n" +
            "update EventState as ES set restless = true\n" +
            "where ES.ProcessModelID = a.pmID and ES.caseID = a.caseID and ES.eventID=\"%s\";\n";
    private final Document document;
    public DCRRuleGenerator(long pmID, long caseID,File file) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        document = builder.parse(file);

        this.processModelID = pmID;
        this.caseID = caseID;
    }
    @Override
    public String generateEPLModule() {


        DCRGraph graph = buildDCRGRaph();

        if (!graph.isEmpty())
        {
            StringBuilder result = new StringBuilder(DEFINE_CONTEXT);
            result.append(EVENT_STATE_TABLE);
            result.append(DEFINE_TRACK_EVENTS);
            result.append(UPDATE_EVENT_STATE);
            for (Iterator<Event> it = graph.getNodes(); it.hasNext(); ) {
                Event e = it.next();
                result.append(String.format(INITIAL_EVENT_STATE,e.getName(),"false",!e.isExcluded(),"false"));

                Set<Event> preConditions = graph.getPreConditionNodes(e);
                if (!preConditions.isEmpty())
                {
                    StringBuilder  preConditionRule = new StringBuilder(String.format("// precondition rule\n @Priority(5) context partitionedByPmIDAndCaseID on ProcessEvent(nodeID=\"%s\") as a\n" +
                            "update EventState as ES set restless = false, happened=true\n" +
                            "where ES.ProcessModelID = a.pmID and ES.caseID = a.caseID and ES.eventID=\"%s\"\n",e.getName(),e.getName()));

                    for (Event preConditionEvent : preConditions)
                    {
                        preConditionRule.append(String.format(" and exists (select 1 from EventState as ES2 where ES2.eventID = \"%s\" and ES2.caseID = ES.caseID \n" +
                                                                "and (not ES2.included or (ES.included and ES2.happened)))\n",preConditionEvent.getName()));
                    }
                    preConditionRule.append(";\n");

                    result.append(preConditionRule);

                }

            }

            for (Iterator<Relation> it = graph.getEdges(); it.hasNext();)
            {
                Relation r = it.next();
                if (r.getRelationType() == RelationType.EXCLUDE)
                {
                    result.append(String.format(EXCLUDE_RULE,r.getSource().getName(),
                                                            r.getDestination().getName(),
                                                            r.getSource().getName(),
                                                            r.getDestination().getName()));
                }
                else if (r.getRelationType() == RelationType.INCLUDE)
                {
                    result.append(String.format(INCLUDE_RULE,r.getSource().getName(),
                            r.getDestination().getName(),
                            r.getSource().getName(),
                            r.getDestination().getName()));
                }
                else if (r.getRelationType() == RelationType.RESPONSE)
                {
                    result.append(String.format(RESPONSE_RULE,r.getSource().getName(),
                            r.getDestination().getName(),
                            r.getSource().getName(),
                            r.getDestination().getName()));
                }
            }


            return result.toString();
        }

        return "";
    }

    private DCRGraph buildDCRGRaph() {
        DCRGraph dcrGraph = new DCRGraph();


        //Get nodes == events
        getNodes(dcrGraph);

        //Initially included events == nodes
        labelInitiallyIncludedEvents(dcrGraph);

        //Relationships
        //Response
        getRelationsByType("response", dcrGraph, RelationType.RESPONSE);

        //condition
        getRelationsByType("condition", dcrGraph, RelationType.CONDITION);

        getRelationsByType("include", dcrGraph, RelationType.INCLUDE);

        getRelationsByType("exclude", dcrGraph, RelationType.EXCLUDE);

        getRelationsByType("milestone", dcrGraph, RelationType.MILESTONE);

        return dcrGraph;
    }

    private void getRelationsByType(String response, DCRGraph dcrGraph, RelationType relType) {
        NodeList responseRelationships = document.getElementsByTagName(response);

        for (int i = 0; i < responseRelationships.getLength(); i++) {

            Element elem = (Element) responseRelationships.item(i);

            dcrGraph.addEdge(new Relation(
                    dcrGraph.getNodeByID((elem.getAttribute("sourceId"))),
                    dcrGraph.getNodeByID((elem.getAttribute("targetId"))),
                    relType));
        }
    }

    private void labelInitiallyIncludedEvents(DCRGraph dcrGraph) {
        Node included = document.getElementsByTagName("included").item(0);

        NodeList includedEvents = included.getChildNodes();
        for (int i =0; i < includedEvents.getLength();i++)
        {


            Node nd = includedEvents.item(i);
            if (nd.getNodeType() != Node.ELEMENT_NODE)
                continue;

            Element elem = (Element) nd;
//            System.out.println(elem.getNodeName());
            dcrGraph.getNodeByID((elem.getAttribute("id"))).setExcluded(false);

        }
    }

    private void getNodes(DCRGraph dcrGraph) {
        Node events = document.getElementsByTagName("events").item(0);
//        System.out.println(events.toString());
        NodeList eventXMLNodes = events.getChildNodes();

//        System.out.println(eventXMLNodes.getLength());
        for (int i =0; i < eventXMLNodes.getLength();i++)
        {
            Node elem = eventXMLNodes.item(i);
            if (elem.getNodeType() != Node.ELEMENT_NODE)
                continue;
//            System.out.println(elem.getNodeName());
            Element elem2 = (Element) elem;
            dcrGraph.addNode(new Event(true, elem2.getAttribute("id"),elem2.getAttribute("id")));
        }
        // Label mapping to give names to events
        NodeList idMapping = document.getElementsByTagName("labelMapping");

        for (int i =0; i < idMapping.getLength();i++)
        {

            Element elem = (Element) idMapping.item(i);

            dcrGraph.getNodeByID((elem.getAttribute("eventId"))).setName(elem.getAttribute("labelId"));

        }

    }
}
