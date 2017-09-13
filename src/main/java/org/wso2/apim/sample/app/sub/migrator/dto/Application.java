package org.wso2.apim.sample.app.sub.migrator.dto;

public class Application {
    private int id;
    private String name;
    private String ownerName;
    private String applicationTier;
    private String description;
    private String groupId;
    private String callbackUrl;
    private String createdBy;
    private String applicationStatus;
    
    public void setName(String name) {
        this.name = name;
    }

    public void setApplicationTier(String applicationTier) {
        this.applicationTier = applicationTier;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setApplicationStatus(String applicationStatus) {
        this.applicationStatus = applicationStatus;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getApplicationStatus() {
        return applicationStatus;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getName() {
        return name;
    }

    public String getApplicationTier() {
        return applicationTier;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public String getDescription() {
        return description;
    }

    public String getGroupId() {
        return groupId;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return "Application {\n" +
                "    id: " + toIndentedString(id) + "\n" +
                "    name: " + toIndentedString(name) + "\n" +
                "    ownerName: " + toIndentedString(ownerName) + "\n" +
                "    applicationTier: " + toIndentedString(applicationTier) + "\n" +
                "    description: " + toIndentedString(description) + "\n" +
                "    groupId: " + toIndentedString(groupId) + "\n" +
                "    callbackUrl: " + toIndentedString(callbackUrl) + "\n" +
                "    applicationStatus: " + toIndentedString(applicationStatus) + "\n" +
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
