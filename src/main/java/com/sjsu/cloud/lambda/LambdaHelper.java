package com.sjsu.cloud.lambda;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.AmazonRekognitionException;
import com.amazonaws.services.rekognition.model.CompareFacesMatch;
import com.amazonaws.services.rekognition.model.CompareFacesRequest;
import com.amazonaws.services.rekognition.model.CompareFacesResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectResult;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.sjsu.cloud.model.Output;
import org.apache.commons.lang.RandomStringUtils;

public class LambdaHelper {

	public static final int RETURN_SUCCESS = 200;
	public static final int RETURN_ERROR = 500;
	public static final String AWS_CLOUD_DRIVETIME_BUCKET = "cloud-project-drivetime";
	public static final String AWS_USER_DB_BUCKET = "driving-user-db";

	private AmazonS3 s3;
	//private S3Client s3Client;
	private static final String JPEG = ".jpeg";
	private static final String accessKey = "UPDATE ME";
	private static final String secretAccessKey = "UPDATE ME";

	void initializeAWSConnection() {
//		AwsBasicCredentials awsBasicCredentials = AwsBasicCredentials.create(accessKey, secretAccessKey);
/*
		s3Client = S3Client.builder().credentialsProvider(StaticCredentialsProvider.create(awsBasicCredentials))
				.region(Region.US_WEST_1).build();
*/
		AWSCredentials cred = new BasicAWSCredentials(accessKey, secretAccessKey);
		s3 = new AmazonS3Client(cred);
	}

	String uploadFileToS3Bucket(String imageString, String bucketName, String fileName) {
		String uploadSuccess = null;
		System.out.println("Inside UploadFileToS3Bucket");
		try {
			byte[] imageBytes = Base64.getDecoder().decode(imageString);
			InputStream inputStream = new ByteArrayInputStream(imageBytes);
			ObjectMetadata objectMetadata = new ObjectMetadata();
			objectMetadata.setContentLength(Long.valueOf(imageBytes.length));
			PutObjectResult putObject = s3.putObject(bucketName,fileName,
					inputStream, objectMetadata);
			if(null != putObject.getETag())
				uploadSuccess = fileName;
		} catch (Exception e) {
			System.out.println("Error in upload file to S3 "+ e.getMessage());
		}
		return uploadSuccess;
	}

	public Output compareFaces() {
		Output output = null;
		ArrayList<String> inputimage = new ArrayList();
		ArrayList<String> listofdbimages = new ArrayList();
		//bucket with list of bd images to be compared
		ListObjectsV2Request req1 = new ListObjectsV2Request().withBucketName(AWS_USER_DB_BUCKET).withDelimiter("/");
		ListObjectsV2Result listing1 = s3.listObjectsV2(req1);


		List<S3ObjectSummary> objects1 = listing1.getObjectSummaries();
		for (S3ObjectSummary os: objects1) {
			listofdbimages.add(os.getKey());
			System.out.println("222 "+os.getKey());

		}
		//bucket with input image
		ListObjectsV2Request req2 = new ListObjectsV2Request().withBucketName(AWS_CLOUD_DRIVETIME_BUCKET).withDelimiter("/");
		ListObjectsV2Result listing2 = s3.listObjectsV2(req2);


		List<S3ObjectSummary> objects2 = listing2.getObjectSummaries();
		for (S3ObjectSummary os: objects2) {
			inputimage.add(os.getKey());
			System.out.println("333 "+os.getKey());

		}

		AmazonRekognition rekognitionClient;// = AmazonRekognitionClientBuilder.standard().withRegion("us-west-1").build();

		rekognitionClient = AmazonRekognitionClientBuilder.standard()
				.withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretAccessKey)))
				.withRegion("us-west-1").build();

		for(String i: listofdbimages) {
			CompareFacesRequest compareFacesRequest = new CompareFacesRequest().withSourceImage(new Image()
					.withS3Object(new S3Object()
							.withName(inputimage.get(0)).withBucket(AWS_CLOUD_DRIVETIME_BUCKET))).withTargetImage(new Image()
					.withS3Object(new S3Object()
							.withName(i).withBucket(AWS_USER_DB_BUCKET))).withSimilarityThreshold(80F);

			try {

				CompareFacesResult result= rekognitionClient.compareFaces(compareFacesRequest);
				List<CompareFacesMatch> lists= result.getFaceMatches();

				System.out.println("Detected labels for " + inputimage.get(0)+ " and "+i);

				if(!lists.isEmpty()){
					for (CompareFacesMatch label: lists) {
						System.out.println(label.getFace() + ": Similarity is " + label.getSimilarity().toString());
						output = new Output();
						output.setFile1(i);
						output.setFile2(inputimage.get(0));
						output.setSimilarity(Float.parseFloat(label.getSimilarity().toString()));
						if(label.getSimilarity()>90.0) {
							System.out.println("Files compared "+i +"&"+inputimage.get(0));
							output = new Output();
							output.setFile1(i);
							output.setFile2(inputimage.get(0));
							output.setSimilarity(Float.parseFloat(label.getSimilarity().toString()));
							return output;
						}
					}
				}else{
					System.out.println("Faces Does not match");
				}
			} catch(AmazonRekognitionException e) {

				System.out.println("Exception in rekognition "+ e.getMessage() +" file "+ i +"  "+inputimage.get(0) );
				e.printStackTrace();
			}
		}
		if(null == output){
			output = new Output();
			output.setFile1(inputimage.get(0));
			output.setSimilarity(0);
		}
		return output;

	}

	void deleteFileFromS3(String resultFile) {
		System.out.println("Inside delete file");
		try {
			DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(
					AWS_CLOUD_DRIVETIME_BUCKET, resultFile);
			s3.deleteObject(deleteObjectRequest);
			System.out.println("delete completed");
		} catch (AmazonServiceException ex) {
			System.out.println("Exception in delete");
			ex.printStackTrace();
		}
	}
}
