/*
 * Copyright (C) 2024 Not Alexa
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
package not.alexa.netobjects.coding.protobuf;

import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.AbstractCodingScheme;
import not.alexa.netobjects.coding.CodingScheme;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.coding.Encoder;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.DeferredObject;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.PrimitiveTypeDefinition;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.AccessContext;
import not.alexa.netobjects.types.access.AccessFactory;
import not.alexa.netobjects.utils.WeakReferenceKeyMap;

/**
 * Entrypoint to protobuf serialization/deserialization. This coding scheme encodes objects into
 * the protobuf wire format with the following features:
 * <ul>
 * <li>Native protobuf support. If native protobuf messages are used, put the protobuf library into
 * the class path. The library implements resolution of protobuf messages into a {@link TypeDefinition}
 * (allowing protobuf messages to be serialized into XML, Json, YAML and perhaps others). Additionally, the
 * library provides a link between native protobuf encoding and this encoder using native encoding
 * whenever possible.
 * <li>The coding scheme honours the hints {@code protobuf:signed} to encode in zigzag format and
 * {@code protobuf:fixed} to encode in fixed format.
 * <li>The coding scheme honours interfaces and abstract types using "any" as follows: The object is wrapped
 * into a message with first element a type definition and second element the encoded object.
 * Note that the encoding of the second field can be any encoding type since the field can be of any type (including
 * enumerations, integers etc.).
 * <li>The coding scheme supports references as follows: If an object with enabled object refs is serialized,
 * a reference to this object is created. Next time, the reference (an integer) is serialized
 * into the stream using the integer encoding (which differs from the first encoding with variable length).
 * </ul>
 * 
 * @author notalexa
 */
public class ProtobufCodingScheme extends AbstractCodingScheme implements CodingScheme {
	private static final NativeProtobufSupport NATIVE_SUPPORT;
	static {
		NativeProtobufSupport support;
		try {
			support=(NativeProtobufSupport)Class.forName("not.alexa.netobjects.protobuf.NativeProtobufSupport").newInstance();
		} catch(Throwable t) {
			support=new NativeProtobufSupport() {
			};
		}
		NATIVE_SUPPORT=support;
	}
	public static final ClassTypeDefinition ANY=new ClassTypeDefinition().createBuilder()
			.addField("type",PrimitiveTypeDefinition.getTypeDescription(ObjectType.class))
			.addField("content",PrimitiveTypeDefinition.getTypeDescription(byte[].class))
			.build();
	private static Map<Class<?>,PrimitiveTypeCodec> PRIMITIVE_CODECS=new HashMap<Class<?>,PrimitiveTypeCodec>() {

		private static final long serialVersionUID = 1L;

		@Override
		public PrimitiveTypeCodec get(Object key) {
			PrimitiveTypeCodec codec=super.get(key);
			if(codec==null&&ObjectType.class.isAssignableFrom((Class<?>)key)) {
				return PrimitiveTypeCodecs.OBJECT_TYPE_CODEC;
			} else {
				return codec;
			}
		}
		
	};
	private static Map<Class<?>,PrimitiveTypeCodec> ZIG_ZAG_CODECS=new HashMap<Class<?>,PrimitiveTypeCodec>() {

		private static final long serialVersionUID = 1L;

		@Override
		public PrimitiveTypeCodec get(Object arg0) {
			PrimitiveTypeCodec codec=super.get(arg0);
			return codec==null?PRIMITIVE_CODECS.get(arg0):codec;
		}
	};
	private static Map<Class<?>,PrimitiveTypeCodec> FIXED_LENGTH_CODECS=new HashMap<Class<?>,PrimitiveTypeCodec>() {

		private static final long serialVersionUID = 1L;

		@Override
		public PrimitiveTypeCodec get(Object arg0) {
			PrimitiveTypeCodec codec=super.get(arg0);
			return codec==null?PRIMITIVE_CODECS.get(arg0):codec;
		}
	};
	private WeakReferenceKeyMap<Access,AbstractCodec> classCodecs=new WeakReferenceKeyMap<>();
	
	static {
		PRIMITIVE_CODECS.put(ObjectType.class, PrimitiveTypeCodecs.OBJECT_TYPE_CODEC);
		PRIMITIVE_CODECS.put(UUID.class, PrimitiveTypeCodecs.UUID_CODEC);
		PRIMITIVE_CODECS.put(BigInteger.class, PrimitiveTypeCodecs.BIGINTEGER_CODEC);
		PRIMITIVE_CODECS.put(BigDecimal.class, PrimitiveTypeCodecs.BIGDECIMAL_CODEC);
		PRIMITIVE_CODECS.put(String.class, PrimitiveTypeCodecs.STRING_CODEC);
		PRIMITIVE_CODECS.put(Date.class, PrimitiveTypeCodecs.DATE_CODEC);
		PRIMITIVE_CODECS.put(Boolean.class, PrimitiveTypeCodecs.BOOLEAN_CODEC);
		PRIMITIVE_CODECS.put(Boolean.TYPE, PrimitiveTypeCodecs.BOOLEAN_CODEC);
		PRIMITIVE_CODECS.put(Character.class, PrimitiveTypeCodecs.CHARACTER_CODEC);
		PRIMITIVE_CODECS.put(Character.TYPE, PrimitiveTypeCodecs.CHARACTER_CODEC);
		PRIMITIVE_CODECS.put(Byte.class, PrimitiveTypeCodecs.BYTE_CODEC);
		PRIMITIVE_CODECS.put(Byte.TYPE, PrimitiveTypeCodecs.BYTE_CODEC);
		PRIMITIVE_CODECS.put(Short.class, PrimitiveTypeCodecs.SHORT_CODEC);
		PRIMITIVE_CODECS.put(Short.TYPE, PrimitiveTypeCodecs.SHORT_CODEC);
		PRIMITIVE_CODECS.put(Integer.class, PrimitiveTypeCodecs.INTEGER_CODEC);
		PRIMITIVE_CODECS.put(Integer.TYPE, PrimitiveTypeCodecs.INTEGER_CODEC);
		PRIMITIVE_CODECS.put(Long.class, PrimitiveTypeCodecs.LONG_CODEC);
		PRIMITIVE_CODECS.put(Long.TYPE, PrimitiveTypeCodecs.LONG_CODEC);
		PRIMITIVE_CODECS.put(Float.class, PrimitiveTypeCodecs.FLOAT_CODEC);
		PRIMITIVE_CODECS.put(Float.TYPE, PrimitiveTypeCodecs.FLOAT_CODEC);
		PRIMITIVE_CODECS.put(Double.class, PrimitiveTypeCodecs.DOUBLE_CODEC);
		PRIMITIVE_CODECS.put(Double.TYPE, PrimitiveTypeCodecs.DOUBLE_CODEC);
		PRIMITIVE_CODECS.put(byte[].class, PrimitiveTypeCodecs.BYTEARRAY_CODEC);
		
		ZIG_ZAG_CODECS.put(Byte.class, PrimitiveTypeCodecs.ZIG_ZAG_BYTE_CODEC);
		ZIG_ZAG_CODECS.put(Byte.TYPE, PrimitiveTypeCodecs.ZIG_ZAG_BYTE_CODEC);
		ZIG_ZAG_CODECS.put(Short.class, PrimitiveTypeCodecs.ZIG_ZAG_SHORT_CODEC);
		ZIG_ZAG_CODECS.put(Short.TYPE, PrimitiveTypeCodecs.ZIG_ZAG_SHORT_CODEC);
		ZIG_ZAG_CODECS.put(Integer.class, PrimitiveTypeCodecs.ZIG_ZAG_INTEGER_CODEC);
		ZIG_ZAG_CODECS.put(Integer.TYPE, PrimitiveTypeCodecs.ZIG_ZAG_INTEGER_CODEC);
		ZIG_ZAG_CODECS.put(Long.class, PrimitiveTypeCodecs.ZIG_ZAG_LONG_CODEC);
		ZIG_ZAG_CODECS.put(Long.TYPE, PrimitiveTypeCodecs.ZIG_ZAG_LONG_CODEC);

		FIXED_LENGTH_CODECS.put(Byte.class, PrimitiveTypeCodecs.FIXED_BYTE_CODEC);
		FIXED_LENGTH_CODECS.put(Byte.TYPE, PrimitiveTypeCodecs.FIXED_BYTE_CODEC);
		FIXED_LENGTH_CODECS.put(Short.class, PrimitiveTypeCodecs.FIXED_SHORT_CODEC);
		FIXED_LENGTH_CODECS.put(Short.TYPE, PrimitiveTypeCodecs.FIXED_SHORT_CODEC);
		FIXED_LENGTH_CODECS.put(Integer.class, PrimitiveTypeCodecs.FIXED_INTEGER_CODEC);
		FIXED_LENGTH_CODECS.put(Integer.TYPE, PrimitiveTypeCodecs.FIXED_INTEGER_CODEC);
		FIXED_LENGTH_CODECS.put(Long.class, PrimitiveTypeCodecs.FIXED_LONG_CODEC);
		FIXED_LENGTH_CODECS.put(Long.TYPE, PrimitiveTypeCodecs.FIXED_LONG_CODEC);
	}
	
	public static final ProtobufCodingScheme REST_SCHEME=new ProtobufCodingScheme();
	public static final ProtobufCodingScheme DEFAULT_SCHEME=new ProtobufCodingScheme().newBuilder().setRootType(Object.class).build();


	public ProtobufCodingScheme() {
		this(AccessFactory.getDefault());
	}
	
	public ProtobufCodingScheme(AccessFactory factory) {
		super(factory);
		mimeType="application/x-protobuf";
		fileExtension="pwf"; // protobuf wire format
	}

	@Override
	public Encoder createEncoder(Context context, OutputStream stream) {
		return new ProtobufEncoder(context,this,stream);
	}

	@Override
	public Decoder createDecoder(Context context, InputStream stream) {
		return new ProtobufDecoder(context,this,stream);
	}

	public Decoder createDecoder(Context context, ProtobufBuffer buffer) {
		return new ProtobufDecoder(context,this,null) {
			@Override
			protected ProtobufBuffer getBuffer() throws BaseException {
				return buffer;
			}
		};
	}

	public PrimitiveTypeCodec getPrimitiveTypeCodec(Class<?> clazz) {
		return getPrimitiveTypeCodec(CodecType.Default,clazz);
	}

	public PrimitiveTypeCodec getPrimitiveTypeCodec(CodecType type,Class<?> clazz) {
		return type.getCodecs().get(clazz);
	}

	public PrimitiveTypeCodec getEnumCodec(Class<?> enumClass) {
		return new EnumCodec(enumClass);
	}
	
	public AbstractCodec getClassCodec(Context context,TypeDefinition def) {
		return getClassCodec(factory.resolve(context, def));
	}

	public AbstractCodec getClassCodec(Access typeAccess) {
		if(typeAccess!=null) {
			AbstractCodec codec=classCodecs.get(typeAccess);
			if(codec==null) {
				switch(typeAccess.getType().getFlavour()) {
					case InterfaceType: codec=getAnyCodec(typeAccess);
						break;
					case ClassType: codec=typeAccess instanceof DeferredObject.ClassAccess?new DeferredCodec(typeAccess):new ClassCodec(typeAccess);
						break;
					case ArrayType: codec=new ArrayCodec(1, typeAccess);
						break;
				}
				if(codec!=null) {
					classCodecs.put(typeAccess,codec);
				}
			}
			return codec;
		} else {
			return null;
		}
	}
	
	public interface PrimitiveTypeCodec {
		public void encode(ProtobufBuffer buffer,int field, Object o);
		public default Object decode(long value) throws BaseException {
			throw new BaseException(BaseException.BAD_REQUEST, "Illegal encoding");
		}
		public default Object decode(byte[] value, int offset, int len) throws BaseException {
			throw new BaseException(BaseException.BAD_REQUEST, "Illegal encoding");
		}
	}
	
	public byte[] getProtobufContent(Object o) {
		return NATIVE_SUPPORT.getProtobufContent(o);
	}

	public AccessibleObject getProtobufObject(AccessContext context,Access fieldAccess, byte[] value, int offset, int len) {
		return NATIVE_SUPPORT.getProtobufObject(context,fieldAccess, value, offset, len);
	}

	public ClassCodec getAnyCodec(Access access) {
		return new AnyCodec(access);
	}
	
	public interface NativeProtobufSupport {
		public default byte[] getProtobufContent(Object o) {
			return null;
		}
		public default AccessibleObject getProtobufObject(AccessContext context,Access fieldAccess, byte[] value, int offset, int len) {
			return null;
		}
		
	}
	public enum CodecType {
		Default(PRIMITIVE_CODECS),
		Signed(ZIG_ZAG_CODECS),
		Fixed(FIXED_LENGTH_CODECS);
		
		Map<Class<?>,PrimitiveTypeCodec> codecs;
		CodecType(Map<Class<?>,PrimitiveTypeCodec> codecs) {
			this.codecs=Collections.unmodifiableMap(codecs);
		}
		
		public Map<Class<?>,PrimitiveTypeCodec> getCodecs() {
			return codecs;
		}
	}
	
	public Builder newBuilder() {
		return new Builder(this);
	}
	
	public static class Builder extends AbstractCodingScheme.Builder<ProtobufCodingScheme, Builder> {
		private Builder(ProtobufCodingScheme scheme) {
			super(scheme);
		}
	}
	
	public interface BufferWriter {
		public void write(ProtobufBuffer buffer,int field);
	}
}
