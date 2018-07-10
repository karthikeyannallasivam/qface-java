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
import com.amazonaws.services.rekognition.model.*;
import com.aws.codestar.projecttemplates.GatewayResponse;
import com.aws.codestar.projecttemplates.model.Customer;
import com.aws.codestar.projecttemplates.model.Offer;
import com.aws.codestar.projecttemplates.model.SearchModel;
import com.aws.codestar.projecttemplates.util.Core;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import sun.misc.BASE64Encoder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Handler for requests to Lambda function.
 */
public class SearchHandler implements RequestHandler<Object, Object> {

    private final BASE64Encoder encoder = new BASE64Encoder();

    @Autowired
    private Core core;

    public Object handleRequest(final Object input, final Context context) {
        Map<String, String> headers = new HashMap<>();
        Map<String, Object> request = (Map) input;
        System.out.println("Input: " + input.toString());
        String error = "";
        if (request.containsKey("body")) {

            System.out.println("body: " + request.get("body").toString());
            JSONObject jsonObj = new JSONObject((String) request.get("body"));
            SearchModel searchModel = new SearchModel();
            if (jsonObj.has("photo")) {
                searchModel.setPhoto((String) jsonObj.get("photo"));
                searchModel.setCollectionId((String) jsonObj.get("collectionId"));
            }
            headers.put("Content- Type", "application/json");
            ObjectMapper mapper = new ObjectMapper();
            try {
                List<Customer> customers = searchImage(searchModel);
                String jsonResult;
                if(customers!=null && customers.size() > 0) {
                    jsonResult = mapper.writeValueAsString(customers);
                    System.out.println("Converted Json response:" + jsonResult);
                    return new GatewayResponse(
                            (jsonResult), headers, 200);
                }
                else{
                    jsonResult = new JSONObject().put("Error","No face Match found").toString();
                    System.out.println("Converted Json response:" + jsonResult);
                    return new GatewayResponse(
                            (jsonResult), headers, 400);
                }

            } catch (Exception e) {
                e.printStackTrace();
                error = e.getMessage();
            }
        }
        return new GatewayResponse(error, headers, 500);
    }

    public List<Customer> searchImage(SearchModel searchModel) throws Exception {

        String collectionId = searchModel.getCollectionId();

        System.out.println("Getting photo: " + searchModel);
        byte[] imgBytes = Base64.getMimeDecoder().decode(searchModel.getPhoto());

        List<Customer> customers = new ArrayList<>();
        List<BufferedImage> croppedImageList = core.detectAndCropList(imgBytes);

        System.out.println("Detected faces size: " + croppedImageList.size());
        for(BufferedImage croppedImage: croppedImageList) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(croppedImage, "jpg", baos);
            byte[] cropedImageInBytes = baos.toByteArray();

            ByteBuffer bytimageBytes = ByteBuffer.wrap(cropedImageInBytes);
            AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.standard().withRegion(Regions.US_EAST_2).build();

            Image image = new Image().withBytes(bytimageBytes);

            SearchFacesByImageRequest searchFacesByImageRequest = new SearchFacesByImageRequest()
                    .withCollectionId(collectionId)
                    .withImage(image)
                    .withFaceMatchThreshold(70F)
                    .withMaxFaces(2);

            SearchFacesByImageResult searchFacesByImageResult =
                    rekognitionClient.searchFacesByImage(searchFacesByImageRequest);

            System.out.println("Faces matching largest face in image from");
            List<FaceMatch> faceImageMatches = searchFacesByImageResult.getFaceMatches();
            AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().withRegion(Regions.US_EAST_2).build();
            DynamoDB dynamoDB = new DynamoDB(client);

            Table cosmoMappingTable = dynamoDB.getTable("CosmoMappingTable");
            Table cosmoFFInfo = dynamoDB.getTable("CosmoFFInfo");

            if (faceImageMatches.size() > 0) {
                for (FaceMatch face : faceImageMatches) {
                    String faceId = face.getFace().getFaceId();
                    System.out.println("face Id:" + faceId);

                    Item cosmoMappedItem = cosmoMappingTable.getItem("FaceId", faceId);

                    if (cosmoMappedItem != null) {
                        System.out.println("Found an item matching in cosmo mapping table: " + cosmoMappedItem.toJSONPretty());
                        Item cosmoFFInfoItem = cosmoFFInfo.getItem("FFid", cosmoMappedItem.getString("FFid"));

                        if (cosmoFFInfoItem != null) {
                            System.out.println("Found an item matching in cosmo FF info table: " + cosmoFFInfoItem.toJSONPretty());
                            Customer customer = new Customer();
                            customer.setFfId(cosmoFFInfoItem.getString("FFid"));
                            customer.setName(cosmoFFInfoItem.getString("Name"));
                            customer.setConfidence(face.getFace().getConfidence().toString());
                            customer.setTier(cosmoFFInfoItem.getString("Tier"));
                            customer.setAvatar(cosmoFFInfoItem.getString("Avatar"));
                            customer.setOffers(generateOffer(cosmoFFInfoItem.getString("Name"),
                                    cosmoFFInfoItem.getString("Tier")));
                            if (!customers.contains(customer)) {
                                customers.add(customer);
                            }
                        } else {
                            byte[] imageBytes = baos.toByteArray();

                            Customer customer = new Customer();
                            customer.setFfId(cosmoMappedItem.getString("FFid"));
                            customer.setName("Anonymous");
                            customer.setConfidence(face.getFace().getConfidence().toString());
                            customer.setTier("");
                            customer.setAvatar("data:image/jpeg;base64," + encoder.encode(imageBytes));
                            customer.setOffers(new ArrayList<>());

                            if (!customers.contains(customer)) {
                                customers.add(customer);
                            }
                            System.out.println("Couldn't find matched ff id in Cosmo ff info table for the face id:" + cosmoMappedItem.getString("FFid"));
                        }
                    } else {
                        System.out.println("Couldn't find matched face id in Cosmo mapping table for the face id:" + faceId);
                    }

                }
            } else {
                // No face matched, index this image
                indexImage(rekognitionClient, dynamoDB, image, collectionId, UUID.randomUUID().toString());
            }
            System.out.println("found customers:" + customers.toString());

        }
        return customers;
        //return faceImageMatches.stream().map(fm -> fm.getFace()).map(face -> face.getFaceId()).collect(Collectors.toList());
    }

    private void indexImage(AmazonRekognition rekognitionClient, DynamoDB dynamoDB, Image image, String collectionId, String pid) {
        ExecutorService executor = Executors.newFixedThreadPool(1);
        Runnable runnableTask = () -> {
            System.out.println("Index new faces with pid " + pid);
            IndexFacesRequest indexFacesRequest = new IndexFacesRequest()
                    .withImage(image)
                    .withCollectionId(collectionId)
                    .withExternalImageId(pid)
                    .withDetectionAttributes("ALL");
            IndexFacesResult result = rekognitionClient.indexFaces(indexFacesRequest);

            Table table = dynamoDB.getTable("CosmoMappingTable");

            for (FaceRecord faceRecord : result.getFaceRecords()) {
                Map<String, String> customerMetaData = new HashMap<>();

                customerMetaData.put("TimeOfEntry", new Date().toString());
                customerMetaData.put("faceRecord", faceRecord.toString());

                Item item = new Item()
                        .withPrimaryKey("FaceId", faceRecord.getFace().getFaceId())
                        .withString("FFid", pid)
                        .withString("Pid", pid)
                        .withMap("MetaData", customerMetaData);
                table.putItem(item);
                System.out.println("New mapping for faceId " + faceRecord.getFace().getFaceId() + " pid " + pid);
            }
        };
        executor.execute(runnableTask);
    }

    private List<Offer> generateOffer(String name, String tier) {
        List<Offer> offers = new ArrayList<>();
        Offer offer = new Offer();
        offer.setOfferId("offer1");
        offer.setTitle("Redeem a free Coffee");
        offer.setDescription("Happy 5th Anniversary " + name + " with Qantas!");
        offers.add(offer);

        if (tier.equals("Gold")) {
            Offer offer1 = new Offer();
            offer1.setOfferId("offer2");
            offer1.setTitle("");
            offer1.setDescription("Use 10,000 FF points to get $10 off");
            offers.add(offer1);
        }
        return offers;
    }

}
