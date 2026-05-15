package not.alexa.netobjects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Entry implements MapTest.E {
    @JsonProperty String topic;
}
