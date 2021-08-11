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
package not.alexa.netobjects.coding;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import not.alexa.netobjects.coding.text.BigDecimalCodec;
import not.alexa.netobjects.coding.text.BigIntegerCodec;
import not.alexa.netobjects.coding.text.BooleanCodec;
import not.alexa.netobjects.coding.text.ByteArrayCodec;
import not.alexa.netobjects.coding.text.ByteCodec;
import not.alexa.netobjects.coding.text.CharacterCodec;
import not.alexa.netobjects.coding.text.DateCodec;
import not.alexa.netobjects.coding.text.DoubleCodec;
import not.alexa.netobjects.coding.text.FloatCodec;
import not.alexa.netobjects.coding.text.IntegerCodec;
import not.alexa.netobjects.coding.text.LongCodec;
import not.alexa.netobjects.coding.text.ObjectTypeCodec;
import not.alexa.netobjects.coding.text.ShortCodec;
import not.alexa.netobjects.coding.text.StringCodec;
import not.alexa.netobjects.coding.text.UUIDCodec;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.access.Access;

/**
 * Class representing a set of codecs by object type.
 * The root set for text codecx can be obtained using @{link {@link #defaultTextCodecs()}. This set is immutable. For a mutable set initialized with all loaded codecs
 * in the current set, use {@link #copy()}.
 * <br>A set of codecs cannot be obtained using a constructor. The only way obtaining new sets is to use the copy method. This ensures that default codecs are properly initialized.
 * 
 * @author notalexa
 *
 */
public class Codecs {
    private static Codecs DEFAULT_TEXT_CODECS=new Codecs() {
        @Override
        public synchronized void put(Object key, Codec codec) {
        }
    };
    static {
        DEFAULT_TEXT_CODECS.codecs.put(ObjectType.createClassType(ObjectType.class),ObjectTypeCodec.INSTANCE);
        DEFAULT_TEXT_CODECS.codecs.put(ObjectType.createClassType(String.class),StringCodec.INSTANCE);
        DEFAULT_TEXT_CODECS.codecs.put(ObjectType.createClassType(Integer.TYPE),IntegerCodec.INSTANCE);
        DEFAULT_TEXT_CODECS.codecs.put(ObjectType.createClassType(Boolean.TYPE),BooleanCodec.INSTANCE);
        DEFAULT_TEXT_CODECS.codecs.put(ObjectType.createClassType(Character.TYPE),CharacterCodec.INSTANCE);
        DEFAULT_TEXT_CODECS.codecs.put(ObjectType.createClassType(Byte.TYPE),ByteCodec.INSTANCE);
        DEFAULT_TEXT_CODECS.codecs.put(ObjectType.createClassType(Short.TYPE),ShortCodec.INSTANCE);
        DEFAULT_TEXT_CODECS.codecs.put(ObjectType.createClassType(Long.TYPE),LongCodec.INSTANCE);
        DEFAULT_TEXT_CODECS.codecs.put(ObjectType.createClassType(Float.TYPE),FloatCodec.INSTANCE);
        DEFAULT_TEXT_CODECS.codecs.put(ObjectType.createClassType(Double.TYPE),DoubleCodec.INSTANCE);
        DEFAULT_TEXT_CODECS.codecs.put(ObjectType.createClassType(UUID.class),UUIDCodec.INSTANCE);
        DEFAULT_TEXT_CODECS.codecs.put(ObjectType.createClassType(BigInteger.class),BigIntegerCodec.INSTANCE);
        DEFAULT_TEXT_CODECS.codecs.put(ObjectType.createClassType(BigDecimal.class),BigDecimalCodec.INSTANCE);
        DEFAULT_TEXT_CODECS.codecs.put(ObjectType.createClassType(Date.class),DateCodec.INSTANCE);
        DEFAULT_TEXT_CODECS.codecs.put(ObjectType.createClassType(byte[].class),ByteArrayCodec.INSTANCE);
    }
   
    /**
     * @return The default text codecs
     */
    public static Codecs defaultTextCodecs() {
        return DEFAULT_TEXT_CODECS;
    }
    
    private Map<Object,Codec> codecs=new HashMap<>();

    private Codecs() {
    }
    
    private Codecs(Codecs codecs) {
        this.codecs.putAll(codecs.codecs);
    }
    
    /**
     * Register a new codec in this set. The method <b>doesn't check type consistency<b>
     * and <b>overrides older versions<b>.
     * 
     * @param key the key for this codec. Typically, the key is obtained by {@link Access#getAccessKey(ObjectType)}.
     * @param codec the codec
     */
    public synchronized void put(Object key, Codec codec) {
        if(codec!=null) {
            codecs.put(key,codec);
        }
    }
    
    /**
     * @return the codec for the given key or <code>null</code> if no codec is registered.
     */
    public synchronized Codec get(Object type) {
        return codecs.get(type);
    }
    
    /**
     * Copy this set of codecs. This is the only way to obtain a new codec set. The new
     * set can be modified but modification are restricted to the new set.
     */
    public Codecs copy() {
        return new Codecs(this);
    }
}
