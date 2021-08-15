package not.alexa.netobjects.coding.text;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import not.alexa.netobjects.coding.text.ISO8601DateFormat;
import not.alexa.netobjects.coding.text.ISO8601DateFormat.Format;
import not.alexa.netobjects.coding.text.ISO8601DateFormat.Precision;

@RunWith(org.junit.runners.Parameterized.class)
public class ISO8601DateFormatTest {

    @Parameters
    public static List<TestConfig> testObjects() {
        return Arrays.asList(new TestConfig[] {
                new TestConfig(new ISO8601DateFormat(Format.ISO2014Long,Precision.Year,TimeZone.getTimeZone("GMT")),"2021",true),
                new TestConfig(new ISO8601DateFormat(Format.ISO2014Long,Precision.Month,TimeZone.getTimeZone("GMT")),"2021-12",true),
                new TestConfig(new ISO8601DateFormat(Format.ISO2014Long,Precision.Day,TimeZone.getTimeZone("GMT")),"2021-12-01",true),
                new TestConfig(new ISO8601DateFormat(Format.ISO2014Long,Precision.Hour,TimeZone.getTimeZone("GMT")),"2021-12-01T08Z",true),
                new TestConfig(new ISO8601DateFormat(Format.ISO2014Long,Precision.Minute,TimeZone.getTimeZone("GMT")),"2021-12-01T08:55Z",true),
                new TestConfig(new ISO8601DateFormat(Format.ISO2014Long,Precision.Second,TimeZone.getTimeZone("GMT")),"2021-12-01T08:55:10Z",true),
                new TestConfig(new ISO8601DateFormat(Format.ISO2014Long,Precision.Millisecond,TimeZone.getTimeZone("GMT")),"2021-05-21T23:59:59,1Z",true),
                new TestConfig(new ISO8601DateFormat(Format.ISO2014Long,Precision.Millisecond,TimeZone.getTimeZone("GMT")),"2021-05-21T23:59:59,01Z",true),
                new TestConfig(new ISO8601DateFormat(Format.ISO2014Long,Precision.Millisecond,TimeZone.getTimeZone("GMT")),"2021-05-21T23:59:59,001Z",true),
                new TestConfig(new ISO8601DateFormat(Format.ISO2014Short,Precision.Millisecond,TimeZone.getTimeZone("GMT")),"20210521T135600Z",true),
                new TestConfig(new ISO8601DateFormat(Format.ISO2015Long,Precision.Week,TimeZone.getTimeZone("GMT")),"2021-W22",true),
                new TestConfig(new ISO8601DateFormat(Format.ISO2015Long,Precision.Day,TimeZone.getTimeZone("GMT")),"2021-W22-7",true),
                new TestConfig(new ISO8601DateFormat(Format.ISO2015Long,Precision.Millisecond,TimeZone.getTimeZone("GMT")),"2021-W02-1T00:01:00Z",true),
                new TestConfig(new ISO8601DateFormat(Format.ISO2015Short,Precision.Millisecond,TimeZone.getTimeZone("GMT")),"2021W021T000225Z",true),
                new TestConfig(new ISO8601DateFormat(Format.ISO2711Long,Precision.Millisecond,TimeZone.getTimeZone("GMT")),"2021-010T23:59:59Z",true),
                new TestConfig(new ISO8601DateFormat(Format.ISO2711Long,Precision.Day,TimeZone.getTimeZone("GMT")),"2021-010",true),
                new TestConfig(new ISO8601DateFormat(Format.ISO2711Long,Precision.Millisecond,TimeZone.getTimeZone("GMT+0230")),"2021-010T17:00:45+02:30",true),
                new TestConfig(new ISO8601DateFormat(Format.ISO2711Short,Precision.Millisecond,TimeZone.getTimeZone("GMT+0230")),"2021010T170045+0230",true),
                new TestConfig(new ISO8601DateFormat(Format.ISO2711Short,Precision.Millisecond,TimeZone.getTimeZone("GMT-02")),"2021010T170045-02",true),
                new TestConfig(new ISO8601DateFormat(),"202101000T170045-02",false),
                new TestConfig(new ISO8601DateFormat(),"202-10102T170045-02",false),
                new TestConfig(new ISO8601DateFormat(),"20210102T170:045-02",false),
                new TestConfig(new ISO8601DateFormat(),"20210102T170045T02",false),
                new TestConfig(new ISO8601DateFormat(),"2021010,2T170045T02",false),
                new TestConfig(new ISO8601DateFormat(),"202T170045",false),
                new TestConfig(new ISO8601DateFormat(),"20210102T1+1",false),
                new TestConfig(new ISO8601DateFormat(),"20210102T170045+1",false),
        });
    }
    
    @Parameter
    public TestConfig config;

    public ISO8601DateFormatTest() {
    }
    
    @Test public void testFormat() {
        try {
            Date d=config.format.parse(config.date);
            if(!config.valid) {
                fail(config.date+" is not a valid date");
            }
            String formatted=config.format.format(d);
            assertEquals("Dates should be the same",config.date,formatted);
        } catch(AssertionError e) {
            throw e;
        } catch(Throwable t) {
            //t.printStackTrace();
            if(config.valid) {
                fail(config.date+" is a valid date.");
            }
        }
    }
    
    public static class TestConfig {
        DateFormat format;
        String date;
        boolean valid;
        public TestConfig(DateFormat format,String date,boolean valid) {
            this.format=format;
            this.date=date;
            this.valid=valid;
        }
    }
}
