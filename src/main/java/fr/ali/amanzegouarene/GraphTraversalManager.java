package fr.ali.amanzegouarene;

import com.google.gson.Gson;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class GraphTraversalManager {

    public static void main(String[] args) {
        String startNodeId = args[0];
        String endNodeId = args[1];
        if (isEmpty(startNodeId) || isEmpty(endNodeId)) {
            System.out.println("Illegal arguments startNodeId node: "+startNodeId+", endNodeId node:"+endNodeId);
            System.exit(1);
        }
        //1: fetch the diagram representation
        BpmnJson bpmnJson = getBpmnJson();

        //2: parse the diagram
        ModelElementInstance startElement = getStartingElementByStartingNodeId(startNodeId, bpmnJson);

        //3: out print the traversal path
//        bfsTraversalToPrintPath(startNodeId, endNodeId, startElement);
        dfsTraversalToPrintPath(startNodeId, endNodeId, startElement);
    }

    private static void dfsTraversalToPrintPath(String startNodeId, String endNodeId, ModelElementInstance startElement) {
        if (startElement == null) {
            System.out.println("Starting node note found !");
            System.exit(1);
        }
        Set<String> visited = new LinkedHashSet<>();

        visited.add(startElement.getAttributeValue("id"));
        dfsFindPathAndPrint(((FlowNode) startElement), endNodeId, visited, startNodeId);
        // If path found, it would be printed by dfsFindPathAndPrint function and process finished, otherwise:
        System.out.printf("No path found, starting from %s and ending to %s%n", startNodeId, endNodeId);
        System.exit(1);
    }

    private static void dfsFindPathAndPrint(FlowNode startNode, final String endNodeId, Set<String> visited, final String startNodeId) {
        String sourceId = startNode.getAttributeValue("id");
        Set<String> cloneVisited = new LinkedHashSet<>();
        cloneVisited.addAll(visited);
        cloneVisited.add(sourceId);
        if(endNodeId.equals(sourceId)){
            System.out.printf("The path from %s to %s is:%n%s", startNodeId, endNodeId, cloneVisited);
            System.exit(0);
        }
        for (SequenceFlow sequenceFlow : startNode.getOutgoing()) {
            if (!cloneVisited.contains(sequenceFlow.getTarget().getAttributeValue("id"))) {
                dfsFindPathAndPrint(sequenceFlow.getTarget(), endNodeId, cloneVisited, startNodeId);
            }
        }


    }

    private static void bfsTraversalToPrintPath(String startNodeId, String endNodeId, ModelElementInstance startElement) {
        if (startElement == null) {
            System.out.println("Starting node note found !");
            System.exit(1);
        }
        Set<String> visited = new LinkedHashSet<>();
        Deque<FlowNode> stack = new ArrayDeque<>();

        boolean pathFound = false;
        visited.add(startElement.getAttributeValue("id"));
        Collection<SequenceFlow> sequenceFlow = ((FlowNode) startElement).getOutgoing();
        sequenceFlow.stream().map(SequenceFlow::getTarget).forEach(stack::push);

        while (!stack.isEmpty() && !pathFound){
            FlowNode currentNode = stack.pop();
            String currentNodeId = currentNode.getAttributeValue("id");
            if (!visited.contains(currentNodeId)) {
                if(endNodeId.equals(currentNodeId)){
                    visited.add(currentNodeId);
                    pathFound = true;
                }else{
                    visited.add(currentNodeId);
                    for (SequenceFlow fn : currentNode.getOutgoing()) {
                        stack.push(fn.getTarget());
                    }
                }
            }
        }
        if(pathFound){
            System.out.printf("The path from %s to %s is:%n%s", startNodeId, endNodeId, visited);
        }else{
            System.out.printf("No path found, starting from %s and ending to %s%n", startNodeId, endNodeId);
            System.exit(1);
        }
    }

    private static ModelElementInstance getStartingElementByStartingNodeId(String startNodeId, BpmnJson bpmnJson) {
        if (bpmnJson == null) {
            System.out.println("Empty bpmn model !");
            System.exit(1);
        }
        InputStream stream = new ByteArrayInputStream(bpmnJson.bpmn20Xml.getBytes());
        BpmnModelInstance bpmnModelInstance = Bpmn.readModelFromStream(stream);
        return bpmnModelInstance.getModelElementById(startNodeId);
    }

    private static BpmnJson getBpmnJson() {
        HttpClient httpClient  = HttpClient.newBuilder().build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://n35ro2ic4d.execute-api.eu-central-1.amazonaws.com/prod/engine-rest/process-definition/key/invoice/xml"))
                .GET()
                .build();
        BpmnJson bpmnJson = null;
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            bpmnJson = new Gson().fromJson(response.body(), BpmnJson.class);
        } catch (IOException | InterruptedException e) {
            System.out.println("Error happened when sending GET request to retrieve data");
            System.out.println(e.getMessage());
            System.exit(1);
        }
        return bpmnJson;
    }

    private static boolean isEmpty(String arg) {
        return arg==null || arg.isBlank();
    }

    private static class BpmnJson {
        private String id;
        private String bpmn20Xml;

        public BpmnJson(String id, String bpmn20Xml) {
            this.id = id;
            this.bpmn20Xml = bpmn20Xml;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getBpmn20Xml() {
            return bpmn20Xml;
        }

        public void setBpmn20Xml(String bpmn20Xml) {
            this.bpmn20Xml = bpmn20Xml;
        }
    }
}
