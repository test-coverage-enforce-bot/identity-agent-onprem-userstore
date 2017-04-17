package org.wso2.carbon.identity.user.store.outbound.model;

import java.io.Serializable;

/**
 * User operations
 */
public class UserOperation implements Serializable {

    private String correlationId;
    private String requestType;
    private String requestData;
    private String responseData;
    private String tenant;
    private String serverNode;

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getRequestType() {
        return requestType;
    }

    public void setRequestType(String requestType) {
        this.requestType = requestType;
    }

    public String getRequestData() {
        return requestData;
    }

    public void setRequestData(String requestData) {
        this.requestData = requestData;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getResponseData() {
        return responseData;
    }

    public void setResponseData(String responseData) {
        this.responseData = responseData;
    }

    public String getServerNode() {
        return serverNode;
    }

    public void setServerNode(String serverNode) {
        this.serverNode = serverNode;
    }
}
