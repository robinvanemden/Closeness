package io.pavlov.closeness;

import com.dreamfactory.model.RecordRequest;
import com.fasterxml.jackson.annotation.JsonProperty;

public class KeyValueModel extends RecordRequest {

    @JsonProperty("key")
    String key;

    @JsonProperty("value")
    String value;

    @JsonProperty("ext")
    String ext;

    @JsonProperty("type")
    String type;

    @JsonProperty("address")
    String address;

    @JsonProperty("android_id")
    String android_id;

    @JsonProperty("device_id")
    String device_id;

    @JsonProperty("systemtime")
    String systemtime;

    public String getSystemtime() {
        return this.systemtime;
    }

    public String toString() {
        String sb = "";
        sb += "class RecordRequest {\n";
        sb += "  value: " + value + "\n";
        sb += "  type: " + type + "\n";
        sb += "  timestamp: " + systemtime + "\n";
        sb += "}\n";
        return sb;
    }

}