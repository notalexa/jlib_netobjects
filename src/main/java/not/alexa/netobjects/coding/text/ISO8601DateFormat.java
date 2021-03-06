/*
 * Copyright (C) 2021 Not Alexa
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package not.alexa.netobjects.coding.text;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParsePosition;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Date format implementing (parts of) the ISO 8601 norm as described in <a href="https://en.wikipedia.org/wiki/ISO_8601">Wikipedia</a>.
 * <br>Different versions are summarized in {@link Format}. Precision can be configured
 * using the {@link Precision} constants.
 * <br>The implementation parses legal dates but is lenient in some cases. For example
 * {@code 2021-03-6} is parsed as {@code 2021-036} which is the 36 day in the year 2021 (which
 * is not intended in most cases). Error handling may change in the future.
 *   
 * @author notalexa
 *
 */
public class ISO8601DateFormat extends DateFormat {
    private static char[] DIGITS=new char[] { '0','1','2','3','4','5','6','7','8','9' };
    private static int[] POW=new int[] { 1,10,100,1000,10000};
    private static int[] M=new int[] { 9,9,7,7,5,5,5,5};
    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    Precision precision;
    Format format;
    public ISO8601DateFormat() {
        this(Precision.Millisecond);
    }
    
    public ISO8601DateFormat(Precision precision) {
        this(Format.ISO2014Long,precision);
    }
    
    public ISO8601DateFormat(Format format,Precision precision) { 
        this(format,precision,null);
    }
    
    public ISO8601DateFormat(Format format,Precision precision,TimeZone timeZone) { 
        this.precision=precision;
        this.format=format;
        calendar=Calendar.getInstance();
        if(timeZone!=null) {
            calendar.setTimeZone(timeZone);
        }
    }

    @Override
    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
        Calendar cal=calendar;//getCalendar();
        cal.setTime(date);
        int year=cal.get(Calendar.YEAR);
        format(toAppendTo,year,4);
        if(precision==Precision.Year) {
            return toAppendTo;
        }
        if(format.isLong()) {
            toAppendTo.append('-');
        }
        switch(format.getFormat()) {
        case 0:
            format(toAppendTo,cal.get(Calendar.MONTH)+1,2);
            if(precision==Precision.Month) {
                return toAppendTo;
            }
            if(format.isLong()) {
                toAppendTo.append('-');
            }
            format(toAppendTo,cal.get(Calendar.DAY_OF_MONTH),2);
            if(precision==Precision.Day||precision==Precision.Week) {
                return toAppendTo;
            }
            break;
        case 1:toAppendTo.append('W');
            format(toAppendTo,cal.get(Calendar.WEEK_OF_YEAR),2);
            if(precision==Precision.Month||precision==Precision.Week) {
                return toAppendTo;
            }
            if(format.isLong()) {
                toAppendTo.append('-');
            }
            format(toAppendTo,cal.get(Calendar.DAY_OF_WEEK),1);
            if(precision==Precision.Day) {
                return toAppendTo;
            }
            break;
        case 2:
            format(toAppendTo,cal.get(Calendar.DAY_OF_YEAR),3);
            if(precision==Precision.Month||precision==Precision.Week||precision==Precision.Day) {
                return toAppendTo;
            }
            break;
        }
        toAppendTo.append('T');
        format(toAppendTo,cal.get(Calendar.HOUR_OF_DAY),2);
        if(precision==Precision.Hour) {
            return appendTimezoneOffset(toAppendTo,cal.get(Calendar.ZONE_OFFSET)+cal.get(Calendar.DST_OFFSET));
        }
        if(format.isLong()) {
            toAppendTo.append(':');
        }
        format(toAppendTo,cal.get(Calendar.MINUTE),2);
        if(precision==Precision.Minute) {
            return appendTimezoneOffset(toAppendTo,cal.get(Calendar.ZONE_OFFSET)+cal.get(Calendar.DST_OFFSET));
        }
        if(format.isLong()) {
            toAppendTo.append(':');
        }
        format(toAppendTo,cal.get(Calendar.SECOND),2);
        if(precision==Precision.Second) {
            return appendTimezoneOffset(toAppendTo,cal.get(Calendar.ZONE_OFFSET)+cal.get(Calendar.DST_OFFSET));
        }
        int c=cal.get(Calendar.MILLISECOND);
        if(c>0) {
            if(format.isLong()) {
                toAppendTo.append(',');
            }
            format1(toAppendTo,cal.get(Calendar.MILLISECOND),3);
        }
        return appendTimezoneOffset(toAppendTo,cal.get(Calendar.ZONE_OFFSET)+cal.get(Calendar.DST_OFFSET));
    }
    
    protected StringBuffer appendTimezoneOffset(StringBuffer buffer,int offset) {
        if(offset==0) {
            return buffer.append('Z');
        } else if(offset>0) {
            buffer.append('+');
        } else {
            buffer.append('-');
            offset=-offset;
        }
        int h=offset/3600000;
        int m=(offset%3600000)/60000;
        format(buffer,h,2);
        if(format.isLong()) {
            buffer.append(':');
        } else if(m==0) {
            return buffer;
        }
        format(buffer,m,2);
        return buffer;
    }

    private void format1(StringBuffer buffer,int v,int digits) {
        v=v%POW[digits];
        for(int i=digits-1;i>=0;i--) {
            buffer.append(DIGITS[v/POW[i]]);
            v=v%POW[i];
            if(v==0) {
                break;
            }
        }
    }
    
    private void format(StringBuffer buffer,int v,int digits) {
        v=v%POW[digits];
        for(int i=digits-1;i>=0;i--) {
            buffer.append(DIGITS[v/POW[i]]);
            v=v%POW[i];
        }
    }

    @Override
    public Date parse(String source, ParsePosition pos) {
        int[] accu=new int[8];
        Format parseFormat=null;
        int index=0;
        int a=0;
        int d=0;
        int s=1;
        char[] characters=source.toCharArray();
        int i=pos.getIndex();
        for(;i<characters.length&&index<8;i++) {
            switch(characters[i]) {
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9': if(index!=4||d<3) {
                    a=10*a+characters[i]-'0';
                    d++;
                }
                if(d==M[index]) {
                    pos.setErrorIndex(i);
                    return null;
                }
                break;
            case '-':if(index==0) {
                    if(d%2!=0) {
                        pos.setErrorIndex(i);
                        return null;
                    }
                    break;
                }
                s=-1;
            case '+':
                accu[index++]=d;
                accu[index++]=a;
                d=a=0;
                index=6;
                break;
            case ':': if((index!=2&&index!=6)||d%2!=0) {
                    pos.setErrorIndex(i);
                    return null;
                }
                break;
            case 'T':if(index==0) {
                    accu[index++]=d;
                    accu[index++]=a;
                    d=a=0;
                } else {
                    pos.setErrorIndex(i);
                    return null;
                }
                break;
            case 'Z':
                accu[index++]=d;
                accu[index++]=a;
                d=a=0;
                index=8;
                break;
            case 'W':if(index==0&&parseFormat==null) {
                    parseFormat=Format.ISO2015Short;
                }
                break;
            case ',':if(index==2) {
                    accu[index++]=d;
                    accu[index++]=a;
                    d=a=0;
                } else {
                    pos.setErrorIndex(i);
                    return null;
                }
                break;
            }
        }
        if(index<8) {
            accu[index++]=d;
            accu[index++]=s*a;
        }
        switch(accu[0]) {
            case 4:accu[1]=10000*accu[1]+0101;
               parseFormat=Format.ISO2014Short;
               break;
            case 6:if(parseFormat==null) {
                    accu[1]=100*accu[1]+1;
                    parseFormat=Format.ISO2014Short;
                } else {
                    // year+week
                    accu[1]=10*accu[1];
                }
                break;
            case 8:
                parseFormat=Format.ISO2014Short;
                break;
            case 7:
                if(parseFormat==null) {
                    parseFormat=Format.ISO2711Short;
                }
                break;
            default:pos.setErrorIndex(i);
                return null;
        }
        switch(accu[2]) {
            case 2:accu[3]*=10000;
                break;
            case 4:accu[3]*=100;
                break;
            case 0:
            case 6:
                break;
            default:pos.setErrorIndex(i);
                return null;
        }
        switch(accu[4]) {
            case 1:accu[5]*=100;
                break;
            case 2:accu[5]*=10;
                break;
            case 0:
                break;
        }
        switch(accu[6]) {
            case 2: accu[7]*=100;
                break;
            case 0:
            case 4:
                break;
            default:pos.setErrorIndex(i);
                return null;
        }
        pos.setIndex(i);
        Calendar cal=calendar;
        TimeZone timeZone=cal.getTimeZone();
        try {
            a=accu[1];
            switch(parseFormat) {
                case ISO2014Long:
                case ISO2014Short:
                    cal.set(Calendar.DAY_OF_MONTH,a%100);
                    a/=100;
                    cal.set(Calendar.MONTH,a%100-1);
                    a/=100;
                    cal.set(Calendar.YEAR,a);
                    break;
                case ISO2015Long:
                case ISO2015Short:
                    cal.set(Calendar.DAY_OF_WEEK,a%10);
                    a/=10;
                    cal.set(Calendar.WEEK_OF_YEAR,a%100);
                    a/=100;
                    cal.set(Calendar.YEAR,a);
                    break;
                case ISO2711Long:
                case ISO2711Short:
                    cal.set(Calendar.DAY_OF_YEAR,a%1000);
                    a/=1000;
                    cal.set(Calendar.YEAR,a);
                    break;
            }
            a=accu[3];
            cal.set(Calendar.SECOND,a%100);
            a/=100;
            cal.set(Calendar.MINUTE,a%100);
            a/=100;
            cal.set(Calendar.HOUR_OF_DAY,a);
            cal.set(Calendar.MILLISECOND,accu[5]);
            int o=(accu[7]%100)+60*(accu[7]/100); // in minutes
            cal.set(Calendar.ZONE_OFFSET,o*60000);
            return cal.getTime();
        } finally {
            cal.setTimeZone(timeZone);
        }
    }
    
    public enum Precision {
        Year,Month,Week,Day,Hour,Minute,Second,Millisecond;
    }
    
    public enum Format {
        ISO2014Short(false,0),ISO2014Long(true,0),
        ISO2015Short(false,1),ISO2015Long(true,1),
        ISO2711Short(false,2),ISO2711Long(true,2);
        
        private boolean longForm;
        private int format;
        private Format(boolean longForm,int format) {
            this.longForm=longForm;
            this.format=format;
        }
        public int getFormat() {
            return format;
        }
        public boolean isLong() {
            return longForm;
        }
    }
}
