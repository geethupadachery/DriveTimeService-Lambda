package com.sjsu.cloud.lambda;

import java.util.Map;
import java.util.Set;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2ProxyRequestEvent;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;


public class UploadImageLambdaHandler implements RequestHandler<APIGatewayV2ProxyRequestEvent, APIGatewayProxyResponseEvent> {




	@Override
	public APIGatewayProxyResponseEvent handleRequest(APIGatewayV2ProxyRequestEvent event, Context context) {
		String fileName;
		LambdaHelper lambdaHelper = new LambdaHelper();
		LambdaLogger logger = context.getLogger();
		logger.log("Inside Handle Request Upload Image" + event);

		Map<String, String> queryParam = event.getQueryStringParameters();
		APIGatewayProxyResponseEvent returnValue = new APIGatewayProxyResponseEvent();
		if(null != queryParam && queryParam.containsKey("filename"))
			fileName = queryParam.get("filename");
		else
			fileName = RandomStringUtils.randomAlphabetic(6)+".jpeg";

		lambdaHelper.initializeAWSConnection();
		String resultFile = lambdaHelper.uploadFileToS3Bucket(event.getBody(), LambdaHelper.AWS_USER_DB_BUCKET,fileName);

		if(!StringUtils.isEmpty(resultFile))
			returnValue.setStatusCode(LambdaHelper.RETURN_SUCCESS);
		else
			returnValue.setStatusCode(LambdaHelper.RETURN_ERROR);
		JSONObject jsonObject = new JSONObject("{ filename : "+resultFile+" }");
		returnValue.setBody(jsonObject.toString());
		returnValue.setIsBase64Encoded(false);
		return returnValue;
	}


}
