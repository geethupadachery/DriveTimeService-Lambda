package com.sjsu.cloud.lambda;

import java.util.HashMap;
import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyRequestEvent;
import com.sjsu.cloud.model.Output;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;

public class FaceComparisonLambdaHandler implements RequestHandler<APIGatewayV2ProxyRequestEvent, APIGatewayProxyResponseEvent> {



	@Override
	public APIGatewayProxyResponseEvent handleRequest(APIGatewayV2ProxyRequestEvent event, Context context) {
		String fileName;
		LambdaHelper lambdaHelper = new LambdaHelper();
		LambdaLogger logger = context.getLogger();
		logger.log("Inside Handle Request Face Comparison " + event);
		APIGatewayProxyResponseEvent returnValue = new APIGatewayProxyResponseEvent();
		Map<String, String> queryParam = event.getQueryStringParameters();
		if(null != queryParam && queryParam.containsKey("filename"))
			fileName = queryParam.get("filename");
		else
			fileName = RandomStringUtils.randomAlphabetic(6)+".jpeg";

		lambdaHelper.initializeAWSConnection();
		String resultFile = lambdaHelper.uploadFileToS3Bucket(event.getBody(), LambdaHelper.AWS_CLOUD_DRIVETIME_BUCKET, fileName);

		if(StringUtils.isEmpty(resultFile)){
			returnValue.setStatusCode(LambdaHelper.RETURN_ERROR);
			return returnValue;
		}
		Output output = lambdaHelper.compareFaces();
		if(null != output)
			returnValue.setStatusCode(LambdaHelper.RETURN_SUCCESS);
		else
			returnValue.setStatusCode(LambdaHelper.RETURN_ERROR);

		lambdaHelper.deleteFileFromS3(resultFile);

		Map<String, String> headers = new HashMap<>();
		headers.put("Content-Type", "application/json");
		headers.put("Accept", "*/*");
		returnValue.setHeaders(headers);
		JSONObject jsonObject = new JSONObject(output);
		returnValue.setBody(jsonObject.toString());
		returnValue.setIsBase64Encoded(false);
		return returnValue;
	}



}
