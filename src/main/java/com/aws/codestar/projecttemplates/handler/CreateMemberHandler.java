package com.aws.codestar.projecttemplates.handler;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.aws.codestar.projecttemplates.GatewayResponse;
import com.aws.codestar.projecttemplates.model.Customer;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class CreateMemberHandler implements RequestHandler<Object, Object> {

    @Override
    public Object handleRequest(Object input, Context context) {
        Map<String, String> headers = new HashMap<>();
        Map<String, Object> request = (Map) input;
        System.out.println("Input: " + input.toString());
        String error = "";
        if (request.containsKey("body")) {

            System.out.println("body: " + request.get("body").toString());
            JSONObject jsonObj = new JSONObject((String) request.get("body"));
            Customer customer = new Customer();
            if (jsonObj.has("name")) {
                customer.setName((String) jsonObj.get("name"));
                customer.setFfId((String) jsonObj.get("ffid"));
                customer.setTier((String) jsonObj.get("tier"));
            }
            headers.put("Content- Type", "application/json");
            try {
                createCustomer(customer);
                return new GatewayResponse(new JSONObject().put("Output", "ok").toString(), headers, 200);
            } catch (Exception e) {
                e.printStackTrace();
                error = e.getMessage();
            }
        }
        return new GatewayResponse(error, headers, 500);
    }

    private void createCustomer(Customer customer) {
        System.out.println("Adding customer " + customer);
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().withRegion(Regions.US_EAST_2).build();
        DynamoDB dynamoDB = new DynamoDB(client);
        Table table = dynamoDB.getTable("CosmoFFInfo");

        Item item = new Item()
                .withPrimaryKey("FFid", customer.getFfId())
                .withString("Name", customer.getName())
                .withString("Tier", customer.getTier())
                .withInt("Points", 0);

        table.putItem(item);

    }

}
