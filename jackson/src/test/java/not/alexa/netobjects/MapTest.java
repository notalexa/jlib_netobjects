package not.alexa.netobjects;

import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.coding.yaml.YamlCodingScheme;
import org.junit.Test;

public class MapTest {
    private static final String OBJECT="class: not.alexa.netobjects.Data\n" +
            "data:\n" +
            "  all:\n" +
            "  - class: not.alexa.netobjects.Entry\n" +
            "  - class: not.alexa.netobjects.Entry\n";

    @Test
    public void test() {
        Context conbtext=Context.createRootContext();
        try(Decoder decoder= YamlCodingScheme.DEFAULT_SCHEME.createDecoder(conbtext,OBJECT.getBytes())) {
            Data data=decoder.decode(Data.class);
            System.out.println(data);


        } catch(BaseException e) {
            e.printStackTrace();
        }

    }

    interface E {

    }
}
