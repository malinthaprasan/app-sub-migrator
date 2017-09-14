package org.wso2.apim.sample.app.sub.migrator.dto;

public class Subscription {
    private int id;
    private String tierId;
    private APIIdentifier apiIdentifier;
    private Application application;
    private String subscriptionStatus;
    private String subscriptionCreatedStatus;
    private String createdBy;

    public void setApiIdentifier(APIIdentifier apiIdentifier) {
        this.apiIdentifier = apiIdentifier;
    }

    public void setApplication(Application application) {
        this.application = application;
    }

    public void setSubscriptionCreatedStatus(String subscriptionCreatedStatus) {
        this.subscriptionCreatedStatus = subscriptionCreatedStatus;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setSubscriptionStatus(String subscriptionStatus) {
        this.subscriptionStatus = subscriptionStatus;
    }

    public void setTierId(String tierId) {
        this.tierId = tierId;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public APIIdentifier getApiIdentifier() {
        return apiIdentifier;
    }

    public Application getApplication() {
        return application;
    }

    public int getId() {
        return id;
    }

    public String getSubscriptionCreatedStatus() {
        return subscriptionCreatedStatus;
    }

    public String getSubscriptionStatus() {
        return subscriptionStatus;
    }

    public String getTierId() {
        return tierId;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    @Override
    public String toString() {
        return "Subscription {\n" +
                "    id: " + toIndentedString(id) + "\n" +
                "    tierId: " + toIndentedString(tierId) + "\n" +
                "    apiIdentifier: " + toIndentedString(apiIdentifier) + "\n" +
                "    application: " + toIndentedString(application) + "\n" +
                "    subscriptionStatus: " + toIndentedString(subscriptionStatus) + "\n" +
                "    subscriptionCreatedStatus: " + toIndentedString(subscriptionCreatedStatus) + "\n" +
                "    createdBy: " + toIndentedString(createdBy) + "\n" +
                "}";
    }

    /**
     * Convert the given object to string with each line indented by 4 spaces
     * (except the first line).
     */
    private String toIndentedString(Object o) {
        if (o == null) {
            return "null";
        }
        return o.toString().replace("\n", "\n    ");
    }
}
