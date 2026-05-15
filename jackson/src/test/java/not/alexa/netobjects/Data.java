package not.alexa.netobjects;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class Data {
    @JsonProperty
    Map<String, MapTest.E[]> data;
}
