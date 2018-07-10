package com.aws.codestar.projecttemplates.util;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.*;
import com.amazonaws.services.rekognition.model.Image;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@Component
public class Core {

    public static BufferedImage detectFace(BufferedImage originalImage, DetectFacesRequest request) {
        AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.standard().withRegion(Regions.US_EAST_2).build();
        List<FaceDetail> faceDetails = null;
        BufferedImage cropBuffered = null;
        try {
            DetectFacesResult result = rekognitionClient.detectFaces(request);
            faceDetails = result.getFaceDetails();

            System.out.println("Detected faces size:"+ faceDetails.size());

            for (FaceDetail faceDetail : faceDetails) {
                System.out.println("Detected faceDetail"+ faceDetail);
                Rectangle rect = ShowBoundingBoxPositions(originalImage.getHeight(), originalImage.getWidth(),
                        faceDetail.getBoundingBox(),
                        result.getOrientationCorrection());
                if (rect.getHeight() != 0 && rect.getWidth() != 0 && rect.getX() != 0 && rect.getY() != 0) {
                    return cropImage(originalImage, rect);
                } else {
                    System.out.println("Face can't be croped");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<BufferedImage> detectFaces(BufferedImage originalImage, DetectFacesRequest request) {
        AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.standard().withRegion(Regions.US_EAST_2).build();
        List<FaceDetail> faceDetails = null;
        List<BufferedImage> cropBufferedList = new ArrayList<>();
        try {
            DetectFacesResult result = rekognitionClient.detectFaces(request);
            faceDetails = result.getFaceDetails();

            System.out.println("Detected faces size:"+ faceDetails.size());

            for (FaceDetail faceDetail : faceDetails) {
                System.out.println("Detected faceDetail"+ faceDetail);
                Rectangle rect = ShowBoundingBoxPositions(originalImage.getHeight(), originalImage.getWidth(),
                        faceDetail.getBoundingBox(),
                        result.getOrientationCorrection());
                if (rect.getHeight() != 0 && rect.getWidth() != 0 && rect.getX() != 0 && rect.getY() != 0) {
                    cropBufferedList.add(cropImage(originalImage, rect));
                } else {
                    System.out.println("Face can't be croped");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return cropBufferedList;
    }

    private static BufferedImage cropImage(BufferedImage src, Rectangle rect) {
        BufferedImage dest = src.getSubimage(rect.x, rect.y, rect.width, rect.height);
        return dest;
    }

    public static Rectangle ShowBoundingBoxPositions(int imageHeight, int imageWidth, BoundingBox box, String rotation) {

        float left = 0;
        float top = 0;
        int faceWidth = (int) (imageWidth * box.getWidth());
        int faceHeight = (int) (imageHeight * box.getHeight());
        int faceLeft = (int) ((box.getLeft() < 0 || box.getLeft() > 1 ) ? 0 : imageWidth * box.getLeft());
        int faceTop = (int) ((box.getTop() < 0 || box.getTop() > 1 ) ? 0 : imageHeight * box.getTop());
        Rectangle rectangle = new Rectangle(faceLeft, faceTop, faceWidth, faceHeight);

        if (rotation == null) {
            System.out.println("No estimated estimated orientation. Check Exif data.");
            return rectangle;
        }
        //Calculate face position based on image orientation.
        switch (rotation) {
            case "ROTATE_0":
                left = imageWidth * box.getLeft();
                top = imageHeight * box.getTop();
                break;
            case "ROTATE_90":
                left = imageHeight * (1 - (box.getTop() + box.getHeight()));
                top = imageWidth * box.getLeft();
                break;
            case "ROTATE_180":
                left = imageWidth - (imageWidth * (box.getLeft() + box.getWidth()));
                top = imageHeight * (1 - (box.getTop() + box.getHeight()));
                break;
            case "ROTATE_270":
                left = imageHeight * box.getTop();
                top = imageWidth * (1 - box.getLeft() - box.getWidth());
                break;
            default:
                System.out.println("No estimated orientation information. Check Exif data.");
                return rectangle;
        }

        //Display face location information.
        System.out.println("rotation: " + rotation);
        System.out.println("Left: " + String.valueOf((int) left));
        System.out.println("Top: " + String.valueOf((int) top));
        System.out.println("Face Width: " + faceWidth);
        System.out.println("Face Height: " + faceHeight);

        rectangle.setRect((int) left,
                (int) top,
                faceWidth,
                faceHeight);

        return rectangle;
    }


    public static BufferedImage detectAndCrop(byte[] imgBytes) throws Exception {
        InputStream in1 = new ByteArrayInputStream(imgBytes);
        BufferedImage originalImage = ImageIO.read(in1);

        ByteBuffer bytimageBytes = ByteBuffer.wrap(imgBytes);
        System.out.println("Original Image width :" + originalImage.getWidth() + " and height:" + originalImage.getHeight());
        DetectFacesRequest request = new DetectFacesRequest()
                .withImage(new Image()
                        .withBytes(bytimageBytes))
                .withAttributes(Attribute.ALL);
        BufferedImage cropedImage = detectFace(originalImage, request);
        return cropedImage;
    }

    public static List<BufferedImage> detectAndCropList(byte[] imgBytes) throws Exception {
        InputStream in1 = new ByteArrayInputStream(imgBytes);
        BufferedImage originalImage = ImageIO.read(in1);

        ByteBuffer bytimageBytes = ByteBuffer.wrap(imgBytes);
        System.out.println("Original Image width :" + originalImage.getWidth() + " and height:" + originalImage.getHeight());
        DetectFacesRequest request = new DetectFacesRequest()
                .withImage(new Image()
                        .withBytes(bytimageBytes))
                .withAttributes(Attribute.ALL);
        return detectFaces(originalImage, request);
    }
}
