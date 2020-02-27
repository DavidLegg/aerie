package gov.nasa.jpl.ammos.mpsa.aerie.merlincli.models;

import gov.nasa.jpl.ammos.mpsa.aerie.merlincli.exceptions.*;
import org.apache.http.*;
import org.apache.http.client.methods.*;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;

import static gov.nasa.jpl.ammos.mpsa.aerie.merlincli.utils.JsonUtilities.prettify;
import static gov.nasa.jpl.ammos.mpsa.aerie.merlincli.utils.JsonUtilities.writeJson;

public class RemotePlanRepository implements PlanRepository {

    private final String baseURL = "http://localhost:27183/plans";
    private final String instancePath = "activities";
    private HttpHandler httpClient;

    public RemotePlanRepository(HttpHandler httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public String createPlan(String planJson) throws InvalidJsonException, InvalidPlanException {
        HttpResponse response;
        try {
            HttpPost request = new HttpPost(baseURL);
            addJsonToRequest(request, planJson);
            response = this.httpClient.execute(request);
        } catch (IOException e) {
            throw new Error(e);
        }

        switch (response.getStatusLine().getStatusCode()) {
            case HttpStatus.SC_CREATED:
                if (!response.containsHeader("location")) throw new ApiContractViolationException("Plan created but location header not found.");

                // ID for the created entity is in the location header
                return response.getFirstHeader("location").getValue();

            case HttpStatus.SC_BAD_REQUEST:
                // TODO: Add information about what was wrong from the response
                throw new InvalidJsonException();

            case HttpStatus.SC_UNPROCESSABLE_ENTITY:
                // TODO: Add information about what was wrong from the response
                throw new InvalidPlanException();

            default:
                // TODO: Make this a more specific Error
                // Should never happen because we don't have any other status codes from the service
                throw new Error("Unexpected status code returned from plan service");
        }
    }

    @Override
    public void updatePlan(String planId, String planUpdateJson) throws PlanNotFoundException, InvalidJsonException, InvalidPlanException {
        HttpResponse response;
        try {
            HttpPatch request = new HttpPatch(String.format("%s/%s", baseURL, planId));
            addJsonToRequest(request, planUpdateJson);
            response = this.httpClient.execute(request);
        } catch (IOException e) {
            throw new Error(e);
        }

        switch (response.getStatusLine().getStatusCode()) {
            case HttpStatus.SC_OK:
                break;

            case HttpStatus.SC_NOT_FOUND:
                throw new PlanNotFoundException();

            case HttpStatus.SC_BAD_REQUEST:
                // TODO: Add information about what was wrong from the response
                throw new InvalidJsonException();

            case HttpStatus.SC_UNPROCESSABLE_ENTITY:
                // TODO: Add information about what was wrong from the response
                throw new InvalidPlanException();

            default:
                // TODO: Make this a more specific Error
                // Should never happen because we don't have any other status codes from the service
                throw new Error("Unexpected status code returned from plan service");
        }
    }

    @Override
    public void deletePlan(String planId) throws PlanNotFoundException {
        HttpResponse response;
        try {
            HttpDelete request = new HttpDelete(String.format("%s/%s", baseURL, planId));
            response = this.httpClient.execute(request);
        } catch (IOException e) {
            throw new Error(e);
        }

        switch (response.getStatusLine().getStatusCode()) {
            case HttpStatus.SC_OK:
                break;

            case HttpStatus.SC_NOT_FOUND:
                throw new PlanNotFoundException();

            default:
                // TODO: Make this a more specific Error
                // Should never happen because we don't have any other status codes from the service
                throw new Error("Unexpected status code returned from plan service");
        }
    }

    @Override
    public void downloadPlan(String planId, String outName) throws PlanNotFoundException {
        HttpResponse response;
        try {
            HttpGet request = new HttpGet(String.format("%s/%s", baseURL, planId));
            response = this.httpClient.execute(request);
        } catch (IOException e) {
            throw new Error(e);
        }

        switch (response.getStatusLine().getStatusCode()) {
            case HttpStatus.SC_OK:
                try {
                    writeJson(response.getEntity().getContent(), Path.of(outName));
                    return;
                } catch (IOException e) {
                    throw new Error(e);
                }

            case HttpStatus.SC_NOT_FOUND:
                throw new PlanNotFoundException();

            default:
                // TODO: Make this a more specific Error
                // Should never happen because we don't have any other status codes from the service
                throw new Error("Unexpected status code returned from plan service");
        }
    }

    @Override
    public String getPlanList() {
        HttpResponse response;
        try {
            HttpGet request = new HttpGet(baseURL);
            response = this.httpClient.execute(request);
        } catch (IOException e) {
            throw new Error(e);
        }

        switch (response.getStatusLine().getStatusCode()) {
            case HttpStatus.SC_OK:
                try {
                    return prettify(new String(response.getEntity().getContent().readAllBytes()));
                } catch (IOException e) {
                    throw new Error(e);
                }

            default:
                // TODO: Make this a more specific Error
                // Should never happen because we don't have any other status codes from the service
                throw new Error("Unexpected status code returned from plan service");
        }
    }

    @Override
    public void appendActivityInstances(String planId, String instanceListJson) throws PlanNotFoundException, InvalidJsonException, InvalidPlanException {
        HttpResponse response;
        try {
            HttpPost request = new HttpPost(String.format("%s/%s", baseURL, planId));
            addJsonToRequest(request, instanceListJson);
            response = this.httpClient.execute(request);
        } catch (IOException e) {
            throw new Error(e);
        }

        switch (response.getStatusLine().getStatusCode()) {
            case HttpStatus.SC_OK:
                break;

            case HttpStatus.SC_NOT_FOUND:
                throw new PlanNotFoundException();

            case HttpStatus.SC_BAD_REQUEST:
                // TODO: Add information about what was wrong from the response
                throw new InvalidJsonException();

            case HttpStatus.SC_UNPROCESSABLE_ENTITY:
                // TODO: Add information about what was wrong from the response
                throw new InvalidPlanException();

            default:
                // TODO: Make this a more specific Error
                // Should never happen because we don't have any other status codes from the service
                throw new Error("Unexpected status code returned from plan service");
        }
    }

    @Override
    public String getActivityInstance(String planId, String activityId) throws ActivityInstanceNotFoundException {
        HttpResponse response;
        try {
            HttpGet request = new HttpGet(String.format("%s/%s/%s/%s", baseURL, planId, instancePath, activityId));
            response = this.httpClient.execute(request);
        } catch (IOException e) {
            throw new Error(e);
        }

        switch (response.getStatusLine().getStatusCode()) {
            case HttpStatus.SC_OK:
                try {
                    return new String(response.getEntity().getContent().readAllBytes());
                } catch (IOException e) {
                    throw new Error(e);
                }

            case HttpStatus.SC_NOT_FOUND:
                // TODO: When the plan service is updated to distinguish between
                //       PlanNotFound or ActivityInstanceNotFound errors, update this
                throw new ActivityInstanceNotFoundException();

            default:
                // TODO: Make this a more specific Error
                // Should never happen because we don't have any other status codes from the service
                throw new Error("Unexpected status code returned from plan service");
        }
    }

    @Override
    public void updateActivityInstance(String planId, String activityId, String activityInstanceJson) throws ActivityInstanceNotFoundException, InvalidJsonException, InvalidActivityInstanceException {
        HttpResponse response;
        try {
            HttpPatch request = new HttpPatch(String.format("%s/%s/%s/%s", baseURL, planId, instancePath, activityId));
            addJsonToRequest(request, activityInstanceJson);
            response = this.httpClient.execute(request);
        } catch (IOException e) {
            throw new Error(e);
        }

        switch (response.getStatusLine().getStatusCode()) {
            case HttpStatus.SC_NO_CONTENT:
                break;

            case HttpStatus.SC_NOT_FOUND:
                // TODO: When the plan service is updated to distinguish between
                //       PlanNotFound or ActivityInstanceNotFound errors, update this
                throw new ActivityInstanceNotFoundException();

            case HttpStatus.SC_BAD_REQUEST:
                // TODO: Add information about what was wrong from the response
                throw new InvalidJsonException();

            case HttpStatus.SC_UNPROCESSABLE_ENTITY:
                // TODO: Add information about what was wrong from the response
                throw new InvalidActivityInstanceException();

            default:
                // TODO: Make this a more specific Error
                // Should never happen because we don't have any other status codes from the service
                throw new Error("Unexpected status code returned from plan service");
        }
    }

    @Override
    public void deleteActivityInstance(String planId, String activityId) throws ActivityInstanceNotFoundException {
        HttpResponse response;
        try {
            HttpDelete request = new HttpDelete(String.format("%s/%s/%s/%s", baseURL, planId, instancePath, activityId));
            response = this.httpClient.execute(request);
        } catch (IOException e) {
            throw new Error(e);
        }

        switch (response.getStatusLine().getStatusCode()) {
            case HttpStatus.SC_OK:
                break;

            case HttpStatus.SC_NOT_FOUND:
                // TODO: When the plan service is updated to distinguish between
                //       PlanNotFound or ActivityInstanceNotFound errors, update this
                throw new ActivityInstanceNotFoundException();

            default:
                // TODO: Make this a more specific Error
                // Should never happen because we don't have any other status codes from the service
                throw new Error("Unexpected status code returned from plan service");
        }
    }

    private void addJsonToRequest(HttpEntityEnclosingRequest request, String json) throws UnsupportedEncodingException {
        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        request.setEntity(new StringEntity(json));
    }
}