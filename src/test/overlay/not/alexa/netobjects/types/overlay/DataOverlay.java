package not.alexa.netobjects.types.overlay;

import not.alexa.coding.Data;
import not.alexa.netobjects.api.Overlay;

@Overlay
public class DataOverlay extends Data {

    public DataOverlay() {
    }

    public DataOverlay(String text, int index, String... list) {
        super(text, index, list);
    }

}
