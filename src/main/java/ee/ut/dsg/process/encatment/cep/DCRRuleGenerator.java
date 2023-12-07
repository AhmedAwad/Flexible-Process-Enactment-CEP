package ee.ut.dsg.process.encatment.cep;

import ee.ut.dsg.process.encatment.cep.dcr.DCRGraph;
import ee.ut.dsg.process.encatment.cep.dcr.Event;
import ee.ut.dsg.process.encatment.cep.dcr.Relation;
import ee.ut.dsg.process.encatment.cep.dcr.RelationType;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;
import java.io.*;



public class DCRRuleGenerator extends RuleGenerator{


    private Document document;
    public DCRRuleGenerator(File file) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        document = builder.parse(file);
    }
    @Override
    public String generateEPLModule() {


        DCRGraph dcrGraph = new DCRGraph();

        Element root = document.getDocumentElement();
        //Get nodes == events
        NodeList eventXMLNodes = document.getElementsByTagName("event");



        for (int i =0; i < eventXMLNodes.getLength();i++)
        {
            Element elem = (Element) eventXMLNodes.item(i);
            dcrGraph.addNode(new Event(true, elem.getAttribute("id"),elem.getAttribute("id")));
        }
        // Label mapping to give names to events
        NodeList idMapping = document.getElementsByTagName("labelMapping");

        for (int i =0; i < idMapping.getLength();i++)
        {

            Element elem = (Element) idMapping.item(i);

            dcrGraph.getNodeByID((elem.getAttribute("eventId"))).setName(elem.getAttribute("labelId"));

        }

        //Initially included events == nodes
        Node included = document.getElementsByTagName("included").item(0);

        NodeList includedEvents = included.getChildNodes();
        for (int i =0; i < includedEvents.getLength();i++)
        {


            Element elem = (Element) includedEvents.item(i);

            dcrGraph.getNodeByID((elem.getAttribute("Id"))).setExcluded(false);

        }

        //Relationships
        //Response
        NodeList responseRelationships = document.getElementsByTagName("response");

        for (int i =0; i < responseRelationships.getLength();i++)
        {

            Element elem = (Element) responseRelationships.item(i);

            dcrGraph.addEdge(new Relation(
                    dcrGraph.getNodeByID((elem.getAttribute("sourceId"))),
                    dcrGraph.getNodeByID((elem.getAttribute("targetId"))),
                    RelationType.RESPONSE));
        }

        //condition
        NodeList conditionRelationships = document.getElementsByTagName("condition");

        for (int i =0; i < conditionRelationships.getLength();i++)
        {

            Element elem = (Element) conditionRelationships.item(i);

            dcrGraph.addEdge(new Relation(
                    dcrGraph.getNodeByID((elem.getAttribute("sourceId"))),
                    dcrGraph.getNodeByID((elem.getAttribute("targetId"))),
                    RelationType.CONDITION));
        }

        NodeList includeRelationships = document.getElementsByTagName("include");

        for (int i =0; i < includeRelationships.getLength();i++)
        {

            Element elem = (Element) includeRelationships.item(i);

            dcrGraph.addEdge(new Relation(
                    dcrGraph.getNodeByID((elem.getAttribute("sourceId"))),
                    dcrGraph.getNodeByID((elem.getAttribute("targetId"))),
                    RelationType.INCLUDE));
        }

        return null;
    }
}
