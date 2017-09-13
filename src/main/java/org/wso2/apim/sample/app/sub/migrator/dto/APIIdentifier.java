package org.wso2.apim.sample.app.sub.migrator.dto;

public class APIIdentifier {
    private int id;
    private String apiName;
    private String version;

    public String getApiName() {
        return apiName;
    }

    public String getVersion() {
        return version;
    }

    public int getId() {
        return id;
    }

    public void setApiName(String apiName) {
        this.apiName = apiName;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "APIIdentifier {\n" +
                "    id: " + toIndentedString(id) + "\n" +
                "    apiName: " + toIndentedString(apiName) + "\n" +
                "    version: " + toIndentedString(version) + "\n" +
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
