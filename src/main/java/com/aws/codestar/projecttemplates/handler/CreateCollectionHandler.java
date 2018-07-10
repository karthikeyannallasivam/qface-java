package com.aws.codestar.projecttemplates.handler;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.CreateCollectionRequest;
import com.amazonaws.services.rekognition.model.CreateCollectionResult;
import com.aws.codestar.projecttemplates.GatewayResponse;
import com.aws.codestar.projecttemplates.util.Core;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

public class CreateCollectionHandler implements RequestHandler<Object, Object> {

    @Autowired
    private Core core;

    @Override
    public Object handleRequest(final Object input, final Context context) {
        Map<String, String> headers = new HashMap<>();
        Map<String, Object> request = (Map) input;
        System.out.println("Input: " + input.toString());
        String error = "";
        if (request.containsKey("body")) {

            System.out.println("body: " + request.get("body").toString());
            JSONObject jsonObj = new JSONObject((String) request.get("body"));

            String collectionId;
            if (jsonObj.has("collectionId")) {
                collectionId = (String) jsonObj.get("collectionId");
            } else {
                return new GatewayResponse("No collection Id provided", headers, 500);
            }
            headers.put("Content- Type", "application/json");
            try {
                AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.standard().withRegion(Regions.US_EAST_2).build();
                CreateCollectionRequest createCollectionRequest = new CreateCollectionRequest();
                createCollectionRequest.setCollectionId(collectionId);
                CreateCollectionResult result = rekognitionClient.createCollection(createCollectionRequest);
                System.out.println("Create collection result: " + result);
                if (result.getStatusCode() == 200) {
                    return new GatewayResponse(new JSONObject().put("Output", result.getCollectionArn()).toString(), headers, 200);
                }
            } catch (Exception e) {
                e.printStackTrace();
                error = e.getMessage();
            }
        }
        return new GatewayResponse(error, headers, 500);
    }

}
