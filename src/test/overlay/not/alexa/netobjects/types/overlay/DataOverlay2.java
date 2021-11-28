package not.alexa.netobjects.types.overlay;

import not.alexa.netobjects.Context;
import not.alexa.netobjects.api.Overlay;

@Overlay
public class DataOverlay2 extends Data2 {

    public DataOverlay2() {
    }

    public DataOverlay2(String text, int index, String... list) {
        super(text, index, list);
    }


    @Override
    public String helloWorld(Context context) throws Throwable {
        return super.helloWorld(context)+" from Overlay";
    }

    @Override
    public String helloUniverse(Context context) throws Throwable {
        return super.helloUniverse(context)+" from Overlay";
    }

}
