package not.alexa.netobjects;

import not.alexa.netobjects.api.NetworkObject;
import not.alexa.netobjects.types.Lambda;

public interface HelloWorldService {
    
    @NetworkObject
    public default String helloWorld(Context context,String text) throws BaseException {
        return new Lambda(this,text) {}.call(context);
    }    
}
