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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;
import java.util.UUID;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.coding.protobuf.ProtobufCodingScheme.PrimitiveTypeCodec;
import not.alexa.netobjects.coding.text.ISO8601DateFormat;
import not.alexa.netobjects.coding.text.ISO8601DateFormat.Format;
import not.alexa.netobjects.coding.text.ISO8601DateFormat.Precision;
import not.alexa.netobjects.types.ObjectType;

/**
 * Holder class for all primitive type codecs. This codecs are imported by {@link ProtobufCodingScheme}.
 * 
 * @author notalexa
 */
class PrimitiveTypeCodecs {
	private static final Charset UTF8=Charset.forName("UTF-8");

	private PrimitiveTypeCodecs() {
	}
	
	static PrimitiveTypeCodec BOOLEAN_CODEC=new PrimitiveTypeCodec() {
		@Override
		public void encode(ProtobufBuffer buffer, int field, Object o) {
			buffer.write(field, o==Boolean.FALSE?0:1);
		}

		@Override
		public Object decode(long value) {
			return value==0?Boolean.FALSE:Boolean.TRUE;
		}
	};
	
	static PrimitiveTypeCodec BYTE_CODEC=new PrimitiveTypeCodec() {
		@Override
		public void encode(ProtobufBuffer buffer, int field, Object o) {
			buffer.write(field, (Byte)o);
		}

		@Override
		public Object decode(long value) {
			return (byte)value;
		}
	};
	
	static PrimitiveTypeCodec SHORT_CODEC=new PrimitiveTypeCodec() {
		@Override
		public void encode(ProtobufBuffer buffer, int field, Object o) {
			buffer.write(field, (Short)o);
		}

		@Override
		public Object decode(long value) {
			return (short)value;
		}
	};
	static PrimitiveTypeCodec INTEGER_CODEC=new PrimitiveTypeCodec() {
		@Override
		public void encode(ProtobufBuffer buffer, int field, Object o) {
			buffer.write(field, (Integer)o);
		}

		@Override
		public Object decode(long value) {
			return (int)value;
		}
	};
	
	static PrimitiveTypeCodec LONG_CODEC=new PrimitiveTypeCodec() {
		@Override
		public void encode(ProtobufBuffer buffer, int field, Object o) {
			buffer.write(field, (Long)o);
		}

		@Override
		public Object decode(long value) {
			return (long)value;
		}
	};

	static PrimitiveTypeCodec ZIG_ZAG_BYTE_CODEC=new PrimitiveTypeCodec() {
		@Override
		public void encode(ProtobufBuffer buffer, int field, Object o) {
			buffer.writeZigZag(field, (Byte)o);
		}

		@Override
		public Object decode(long value) {
			return (byte)ProtobufBuffer.gazGiz(value);
		}
	};
	
	static PrimitiveTypeCodec ZIG_ZAG_SHORT_CODEC=new PrimitiveTypeCodec() {
		@Override
		public void encode(ProtobufBuffer buffer, int field, Object o) {
			buffer.writeZigZag(field, (Short)o);
		}

		@Override
		public Object decode(long value) {
			return (short)ProtobufBuffer.gazGiz(value);
		}
	};
	
	static PrimitiveTypeCodec ZIG_ZAG_INTEGER_CODEC=new PrimitiveTypeCodec() {
		@Override
		public void encode(ProtobufBuffer buffer, int field, Object o) {
			buffer.writeZigZag(field, (Integer)o);
		}

		@Override
		public Object decode(long value) {
			return (int)ProtobufBuffer.gazGiz(value);
		}
	};
	
	static PrimitiveTypeCodec ZIG_ZAG_LONG_CODEC=new PrimitiveTypeCodec() {
		@Override
		public void encode(ProtobufBuffer buffer, int field, Object o) {
			buffer.writeZigZag(field, (Long)o);
		}

		@Override
		public Object decode(long value) {
			return (long)ProtobufBuffer.gazGiz(value);
		}
	};

	static PrimitiveTypeCodec FIXED_BYTE_CODEC=new PrimitiveTypeCodec() {
		@Override
		public void encode(ProtobufBuffer buffer, int field, Object o) {
			buffer.writeFixedInt(field, (Byte)o);
		}

		@Override
		public Object decode(long value) {
			return (byte)value;
		}
	};
	
	static PrimitiveTypeCodec FIXED_SHORT_CODEC=new PrimitiveTypeCodec() {
		@Override
		public void encode(ProtobufBuffer buffer, int field, Object o) {
			buffer.writeFixedInt(field, (Short)o);
		}

		@Override
		public Object decode(long value) {
			return (short)value;
		}
	};
	
	static PrimitiveTypeCodec FIXED_INTEGER_CODEC=new PrimitiveTypeCodec() {
		@Override
		public void encode(ProtobufBuffer buffer, int field, Object o) {
			buffer.writeFixedInt(field, (Integer)o);
		}

		@Override
		public Object decode(long value) {
			return (int)value;
		}
	};
	
	static PrimitiveTypeCodec FIXED_LONG_CODEC=new PrimitiveTypeCodec() {
		@Override
		public void encode(ProtobufBuffer buffer, int field, Object o) {
			buffer.writeFixedLong(field, (Long)o);
		}

		@Override
		public Object decode(long value) {
			return (long)value;
		}
	};

	static PrimitiveTypeCodec STRING_CODEC=new PrimitiveTypeCodec() {
		@Override
		public void encode(ProtobufBuffer buffer, int field, Object o) {
			buffer.write(field, (String)o);
		}

		@Override
		public Object decode(byte[] value, int offset, int len) {
			return new String(value,offset,len,UTF8);
		}
	};
	
	static PrimitiveTypeCodec BYTEARRAY_CODEC=new PrimitiveTypeCodec() {
		@Override
		public void encode(ProtobufBuffer buffer, int field, Object o) {
			buffer.write(field, (byte[])o);
		}

		@Override
		public Object decode(byte[] value, int offset, int len) {
			return Arrays.copyOfRange(value, offset, offset+len);
		}
	};
	
	static PrimitiveTypeCodec OBJECT_TYPE_CODEC=new PrimitiveTypeCodec() {
		@Override
		public void encode(ProtobufBuffer buffer, int field, Object o) {
			buffer.write(field, ((ObjectType)o).getUrn());
		}

		@Override
		public Object decode(byte[] value, int offset, int len) {
			return ObjectType.resolve(new String(value,offset,len,UTF8));
		}
	};
	
	static PrimitiveTypeCodec UUID_CODEC=new PrimitiveTypeCodec() {
		@Override
		public void encode(ProtobufBuffer buffer, int field, Object o) {
			buffer.write(field, o.toString());
		}

		@Override
		public Object decode(byte[] value, int offset, int len) {
			return UUID.fromString(new String(value,offset,len,UTF8));
		}
	};

	static PrimitiveTypeCodec BIGINTEGER_CODEC=new PrimitiveTypeCodec() {
		@Override
		public void encode(ProtobufBuffer buffer, int field, Object o) {
			buffer.write(field, o.toString());
		}

		@Override
		public Object decode(byte[] value, int offset, int len) {
			return new BigInteger(new String(value,offset,len,UTF8));
		}
	};

	static PrimitiveTypeCodec BIGDECIMAL_CODEC=new PrimitiveTypeCodec() {
		@Override
		public void encode(ProtobufBuffer buffer, int field, Object o) {
			buffer.write(field, o.toString());
		}

		@Override
		public Object decode(byte[] value, int offset, int len) {
			return new BigDecimal(new String(value,offset,len,UTF8));
		}
	};
	
	static PrimitiveTypeCodec DATE_CODEC=new PrimitiveTypeCodec() {
		DateFormat format=new ISO8601DateFormat(Format.ISO2014Long,Precision.Millisecond,TimeZone.getTimeZone("GMT"));
		@Override
		public void encode(ProtobufBuffer buffer, int field, Object o) {
			String date;
			synchronized (format) {
				date=format.format((Date)o);
				
			}
			buffer.write(field, date);
		}

		@Override
		public Object decode(byte[] value, int offset, int len) throws BaseException {
			try {
				synchronized (format) {
					return format.parse(new String(value,offset,len,UTF8));
				}
			} catch(ParseException e) {
				return BaseException.throwException(e);
			}
		}
	};
	
	static PrimitiveTypeCodec CHARACTER_CODEC=new PrimitiveTypeCodec() {
		@Override
		public void encode(ProtobufBuffer buffer, int field, Object o) {
			buffer.write(field, ((Character)o).charValue());
		}

		@Override
		public Object decode(long value) {
			return (char)value;
		}
	};

	static PrimitiveTypeCodec FLOAT_CODEC=new PrimitiveTypeCodec() {
		@Override
		public void encode(ProtobufBuffer buffer, int field, Object o) {
			buffer.writeFixedInt(field,Float.floatToIntBits((float)o));
		}

		@Override
		public Object decode(long value) {
			return Float.intBitsToFloat((int)value);
		}
	};

	static PrimitiveTypeCodec DOUBLE_CODEC=new PrimitiveTypeCodec() {
		@Override
		public void encode(ProtobufBuffer buffer, int field, Object o) {
			buffer.writeFixedLong(field,Double.doubleToLongBits((double)o));
		}

		@Override
		public Object decode(long value) {
			return Double.longBitsToDouble(value);
		}
	};
}
