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

import java.io.IOException;
import java.io.OutputStream;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.DefaultCodingSupport;
import not.alexa.netobjects.coding.Encoder;
import not.alexa.netobjects.coding.protobuf.ProtobufCodingScheme.PrimitiveTypeCodec;
import not.alexa.netobjects.types.JavaClass.Type;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.AccessContext;
import not.alexa.netobjects.types.access.RuntimeInfo;

/**
 * Encoder class of the {@link ProtobufCodingScheme}
 * 
 * @author notalexa
 */
class ProtobufEncoder extends DefaultCodingSupport implements Encoder, AccessContext {
	private final ProtobufCodingScheme scheme;
	private OutputStream stream;
	private final ProtobufBuffer buffer;
	private final Context context;

	ProtobufEncoder(Context context,ProtobufCodingScheme scheme,OutputStream stream) {
		this.scheme=scheme;
		this.context=context;
		this.stream=stream;
		this.buffer=new ProtobufBuffer();
	}

	@Override
	public ProtobufCodingScheme getCodingScheme() {
		return scheme;
	}

	@Override
	public void close() throws BaseException {
		flush();
		stream=null;
	}

	@Override
	public void flush() throws BaseException {
		try {
			buffer.writeTo(stream);
		} catch(IOException e) {
			BaseException.throwException(e);
		}
	}
	
	public Context getContext() {
		return context;
	}

	@Override
	public Encoder encode(Object o) throws BaseException {
		if(o!=null) {
			AbstractCodec codec=scheme.getClassCodec(context, scheme.getRootType(context,o.getClass()));
			if(codec==null) {
				PrimitiveTypeCodec primitiveTypeCodec=scheme.getPrimitiveTypeCodec(o.getClass());
				if(primitiveTypeCodec!=null) {
					primitiveTypeCodec.encode(buffer, 1, o);
				} else {
					throw new BaseException(BaseException.BAD_REQUEST,"Encoding object of class "+o.getClass().getSimpleName());
				}
			} else {
				codec.encode(this, buffer, o);
			}
			flush();
		}
		return this;
	}

	@Override
	public <T> T castTo(Context context, Class<T> clazz) {
		return clazz.isInstance(this)?(T)this:null;
	}

	@Override
	public RuntimeInfo resolve(Context context, Type type) {
		return getCodingScheme().getFactory().resolve(context, type);
	}

	@Override
	public Access resolve(Context context, TypeDefinition type) {
		return getCodingScheme().getFactory().resolve(context, type);
	}

	@Override
	public Access resolve(Access referrer, TypeDefinition type) {
		return getCodingScheme().getFactory().resolve(referrer, type);
	}
}
