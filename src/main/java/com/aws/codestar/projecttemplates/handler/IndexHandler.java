package com.aws.codestar.projecttemplates.handler;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.FaceRecord;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.IndexFacesRequest;
import com.amazonaws.services.rekognition.model.IndexFacesResult;
import com.aws.codestar.projecttemplates.GatewayResponse;
import com.aws.codestar.projecttemplates.model.IndexModel;
import com.aws.codestar.projecttemplates.util.Core;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Handler for requests to Lambda function.
 */
public class IndexHandler implements RequestHandler<Object, Object> {

    @Autowired
    private Core core;

    public Object handleRequest(final Object input, final Context context) {
        Map<String, String> headers = new HashMap<>();
        Map<String, String> request = (Map) input;
        System.out.println("Input received:" + input);
        ObjectMapper mapper = new ObjectMapper();
        String jsonInString = request.get("body");
        System.out.println("Input jsonInString:" + jsonInString);

        IndexModel indexModel = null;
        try {
            indexModel = mapper.readValue(jsonInString, IndexModel.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Input indexModel:" + indexModel.toString());
        headers.put("Content-Type", "application/json");
        try {
            indexFaces(indexModel);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new GatewayResponse(new JSONObject().put("Output", "Images are indexed").toString(), headers, 200);
    }

    public List<FaceRecord> indexFaces(IndexModel indexModel) throws Exception {

        String collectionId = indexModel.getCollectionId();

        AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.standard().withRegion(Regions.US_EAST_2).build();
        List<FaceRecord> faceRecords = new ArrayList<>();
        BufferedImage cropedImage = null;
        ByteBuffer imageBytes = null;
        byte[] cropedImageInBytes = null;
        System.out.println("FFID:" + indexModel.getFfId());
        System.out.println("PID" + indexModel.getpId());
        System.out.println("Photos size" + indexModel.getPhotos().size());

        String pid = indexModel.getFfId() != null ? indexModel.getFfId() : UUID.randomUUID().toString();
        System.out.println("Person Id" + pid);
        for (String encodedImage : indexModel.getPhotos()) {
            System.out.println("encoded image" + encodedImage);
            byte[] imgBytes = Base64.getMimeDecoder().decode(encodedImage);

            cropedImage = core.detectAndCrop(imgBytes);

            indexModel.setpId(pid);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(cropedImage, "jpg", baos);
            cropedImageInBytes = baos.toByteArray();

            imageBytes = ByteBuffer.wrap(cropedImageInBytes);

            Image image = new Image()
                    .withBytes(imageBytes);

            IndexFacesRequest indexFacesRequest = new IndexFacesRequest()
                    .withImage(image)
                    .withCollectionId(collectionId)
                    .withExternalImageId(pid)
                    .withDetectionAttributes("ALL");

            IndexFacesResult indexFacesResult = rekognitionClient.indexFaces(indexFacesRequest);

            System.out.println(pid + " added");
            List<FaceRecord> faceRecordsResult = indexFacesResult.getFaceRecords();

            for (FaceRecord faceRecord : faceRecordsResult) {
                System.out.println("Indexed Face detected: Faceid is " + faceRecord.getFace().getFaceId());
                faceRecords.add(faceRecord);
            }
        }

        // put it in db

        if (faceRecords.size() > 0) {
            System.out.println("No Of face Ids " + faceRecords.size() + " for the ffid " + pid);
            AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().withRegion(Regions.US_EAST_2).build();
            DynamoDB dynamoDB = new DynamoDB(client);

            Table table = dynamoDB.getTable("CosmoMappingTable");

            for (FaceRecord faceRecord : faceRecords) {
                Map<String, String> customerMetaData = new HashMap<>();

                customerMetaData.put("TimeOfEntry", new Date().toString());
                customerMetaData.put("faceRecord", faceRecord.toString());

                Item item = new Item()
                        .withPrimaryKey("FaceId", faceRecord.getFace().getFaceId())
                        .withString("FFid", pid)
                        .withString("Pid", pid)
                        .withMap("MetaData", customerMetaData);
                table.putItem(item);
            }

            System.out.println("Face records have been saved in DB");
        }

        return faceRecords;
    }

}
