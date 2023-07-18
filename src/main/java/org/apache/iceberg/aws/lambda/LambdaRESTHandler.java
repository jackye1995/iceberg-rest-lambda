package org.apache.iceberg.aws.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.iceberg.aws.lambda.rest.ImmutableLambdaRESTResponse;
import org.apache.iceberg.aws.lambda.rest.LambdaRESTRequest;
import org.apache.iceberg.aws.lambda.rest.LambdaRESTRequestParser;
import org.apache.iceberg.aws.lambda.rest.LambdaRESTResponse;
import org.apache.iceberg.aws.lambda.rest.LambdaRESTResponseParser;
import org.apache.iceberg.exceptions.RESTException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class LambdaRESTHandler implements RequestStreamHandler {

    @Override
    public void handleRequest(
            InputStream input,
            OutputStream output,
            Context context) throws IOException {
        LambdaRESTRequest lambdaRequest = LambdaRESTRequestParser.fromJsonStream(input);
        context.getLogger().log("Received request: " + LambdaRESTRequestParser.toJson(lambdaRequest, true));
        HttpUriRequestBase httpRequest = new HttpUriRequestBase(lambdaRequest.method(), lambdaRequest.uri());
        lambdaRequest.headers().forEach(httpRequest::setHeader);
        if (lambdaRequest.entity() != null) {
            httpRequest.setEntity(new StringEntity(lambdaRequest.entity(), StandardCharsets.UTF_8));
        }

        try (CloseableHttpClient httpClient = HttpClients.custom().build();
             CloseableHttpResponse response = httpClient.execute(httpRequest)) {
            LambdaRESTResponse lambdaResponse = ImmutableLambdaRESTResponse.builder()
                    .code(response.getCode())
                    .entity(extractResponseBodyAsString(response))
                    .reason(response.getReasonPhrase())
                    .build();
            LambdaRESTResponseParser.writeToStream(output, lambdaResponse);
            context.getLogger().log("Produced response: " + LambdaRESTResponseParser.toJson(lambdaResponse, true));
        } finally {
            input.close();
            output.close();
        }
    }

    private static String extractResponseBodyAsString(CloseableHttpResponse response) {
        try {
            if (response.getEntity() == null) {
                return null;
            }

            return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
        } catch (IOException | ParseException e) {
            throw new RESTException(e, "Failed to convert HTTP response body to string");
        }
    }
}
