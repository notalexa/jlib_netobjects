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

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.coding.protobuf.ProtobufCodingScheme.CodecType;
import not.alexa.netobjects.coding.protobuf.ProtobufDecoder.ArrayEntry;
import not.alexa.netobjects.coding.protobuf.ProtobufDecoder.ClassDefListener;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.access.Access;

/**
 * Class used for encoding an object (a message in protobuf chargon).
 * 
 * @author notalexa
 */
class ClassCodec extends AbstractCodec {
	protected int smallest;
	protected FieldCodec[] fieldCodecs;
	protected FieldCodec[] fieldCodecsMap;
	protected int[] mask;
	protected int count;
	protected Checker checker;
	protected boolean enableObjectRefs;

	ClassCodec(ProtobufCodingScheme scheme,Access classAccess) {
		super(scheme,classAccess);
		try {
			enableObjectRefs=((ClassTypeDefinition)classAccess.getType()).enableObjectRefs();
		} catch(Throwable t) {
		}
		Field[] fields=access.getFields();
		mask=new int[2+(fields.length>>5)];
		for(int i=0;i<fields.length;i++) {
			if(!fields[i].isOptional()||fields[i].getDefaultValue()!=null) try {
				count++;
				Object defaultValue=fields[i].getDefaultValue();
				init(mask,i,fields[i].getNumber(),defaultValue==null?null:classAccess.getFieldAccess(fields[i]).makeDefault(defaultValue));
			} catch(BaseException t) {
				
			}
		}
		fieldCodecs=new FieldCodec[fields.length];
		fieldCodecsMap=new FieldCodec[fields.length];
		for(int i=0;i<fields.length;i++) {
			insert(fieldCodecs[i]=new FieldCodec(fields[i]));
		}
		orderCodecs();
	}
	
	private void orderCodecs() {
		int start=Integer.MAX_VALUE;
		for(int i=0;i<fieldCodecsMap.length;i++) {
			if(fieldCodecsMap[i]==null) {
				throw new RuntimeException("Should not happen. Missing codec at #"+i);
			}
			if(fieldCodecsMap[i].f.getNumber()<start) {
				if(start<Integer.MAX_VALUE) {
					fieldCodecsMap[i].ordered=start;
				}
				start=i;
			} else {
				int rover=start;
				while(true) {
					if(fieldCodecsMap[rover].ordered<0) {
						fieldCodecsMap[rover].ordered=i;
						break;
					} else if(fieldCodecsMap[fieldCodecsMap[rover].ordered].f.getNumber()>fieldCodecsMap[i].f.getNumber()) {
						fieldCodecsMap[i].ordered=fieldCodecsMap[rover].ordered;
						fieldCodecsMap[rover].ordered=i;
						break;
					}
					rover=fieldCodecsMap[rover].ordered;
				}
			}
		}
		smallest=start;
	}
	
	private void insert(FieldCodec codec) {
		int number=codec.f.getNumber()-1;
		int index=number%fieldCodecsMap.length;
		FieldCodec last=fieldCodecsMap[index];
		if(last==null) {
			fieldCodecsMap[index]=codec;
		} else {
			while(last.next>=0) {
				last=fieldCodecsMap[last.next];
			}
			for(int i=0;i<fieldCodecsMap.length;i++) {
				if(fieldCodecsMap[i]==null) {
					fieldCodecsMap[i]=codec;
					last.next=i;
					return;
				}
			}
			throw new RuntimeException("Should not happen. Too many codecs registered");
		}
	}
	
	private FieldCodec getCodec(int n) {
		int index=(n-1)%fieldCodecsMap.length;
		while(fieldCodecsMap[index].f.getNumber()!=n) {
			index=fieldCodecsMap[index].next;
		}
		return fieldCodecsMap[index];
	}
	
	public boolean enableObjectRefs() {
		return enableObjectRefs;
	}
	
	public ClassDefListener createListener(ClassDefListener parent) throws BaseException {
		AccessibleObject instance=access.newAccessible(parent);
		if(enableObjectRefs) {
			parent.addObjectReference(instance);
		}
		return parent.createChild(instance,this);
	}	
	
	private void init(int[] mask,int offset,int number,AccessibleObject defaultValue) {
		int index=offset>>5;
		int m=1<<(offset&31);
		mask[index]|=m;
		checker=new Checker(checker,offset,number,defaultValue);
	}
	
	private boolean isMarked(int[] mask,int offset) {
		int index=offset>>5;
		return (mask[index]&(1<<(offset&31)))!=0;
	}
	
	public boolean isArrayCodec() {
		return false;
	}
	
	public ClassTypeDefinition getType() {
		return (ClassTypeDefinition)access.getType();
	}

	@Override
	public void encode(ProtobufEncoder encoder,ProtobufBuffer buffer, Object o) throws BaseException {
		int rover=smallest;
		while(rover>=0) {
			FieldCodec field=fieldCodecsMap[rover];
			Object val=access.getField(o,field.f);
			if(val!=null) {
				field.encode(encoder,buffer, val);
			}
			rover=field.ordered;
		}
	}
	
	public int[] getMask() {
		return mask;
	}

	@Override
	public void consumeInternal(ClassDefListener listener,int field, long value) throws BaseException {
		AccessibleObject obj=listener.resolveObjectReference((int)value);
		if(obj!=null) {
			listener.consume(listener,field,obj);
		} else {
			throw new BaseException(BaseException.BAD_REQUEST,"Unresolvable reference #"+value);
		}
	}


	@Override
	public void consume(ClassDefListener listener,int field, long value) throws BaseException {
		getCodec(field).consume(listener,field,value);
	}
	
	@Override
	public void consume(ClassDefListener listener, int field, byte[] value, int offset, int len) throws BaseException {
		getCodec(field).consume(listener,field,value,offset,len);
	}

	@Override
	public void consume(ClassDefListener listener, int field, AccessibleObject o) throws BaseException {
		FieldCodec codec=getCodec(field);
		listener.currentObject().setField(codec.f, o);
		listener.mark(codec.offset);
	}

	
	class FieldCodec extends CodecHolder {
		Field f;
		int next=-1;
		int ordered=-1;
		FieldCodec(Field f) {
			super(f.getIndex());
			this.f=f;
		}
		
		private void resolveCodec() throws BaseException {
			CodecType type=CodecType.Default;
			if(f.hasHint("protobuf:fixed")) {
				type=CodecType.Fixed;
			} else if(f.hasHint("protobuf:signed")) {
				type=CodecType.Signed;
			}
			resolveCodec(type,access.getFieldAccess(f));
		}
		
		public boolean encode(ProtobufEncoder encoder,ProtobufBuffer buffer,Object o) throws BaseException {
			if(!f.isDefault(o)) {
				if(!super.encode(encoder, buffer, f.getNumber(),o)) {
					byte[] content=scheme.getProtobufContent(o);
					if(content==null) {
						resolveCodec();
						// Recursion of maximum one
						encode(encoder,buffer,o);
					} else {
						buffer.write(f.getNumber(), content);
					}
				}
			}
			return true;
		}

		public void consume(ClassDefListener listener, int field,long value) throws BaseException {
			if(primitiveTypeCodec!=null) {
				listener.mark(offset);
				listener.currentObject().setField(f, access.getFieldAccess(f).makeAccessible(primitiveTypeCodec.decode(value)));
			} else if(classCodec!=null) {
				classCodec.consumeInternal(listener,field, value);
			} else {
				resolveCodec();
				// Recursion of maximum one
				consume(listener,field,value);
			}
		}
		
		public void consume(ClassDefListener listener, int field,byte[] value, int offset, int len) throws BaseException {
			if(primitiveTypeCodec!=null) {
				listener.mark(this.offset);
				listener.currentObject().setField(f, access.getFieldAccess(f).makeAccessible(primitiveTypeCodec.decode(value,offset,len)));
			} else if(classCodec!=null) {
				if(classCodec.isArrayCodec()) {
					classCodec.consume(listener, field, value, offset, len);
				} else {
					ClassDefListener childListener=classCodec.createListener(listener);//   listener.createChild(classCodec);
					new ProtobufBuffer(value, offset,len).consume(childListener);
					listener.mark(this.offset);
					listener.currentObject().setField(f, childListener.finalized());
				}
			} else {
				AccessibleObject fieldValue=scheme.getProtobufObject(access.getFieldAccess(f),value,offset,len);
				if(fieldValue!=null) {
					listener.mark(this.offset);
					listener.currentObject().setField(f, fieldValue);
				} else {
					resolveCodec();
					// Recursion of maximum one
					consume(listener,field,value,offset,len);
				}
			}
		}
	}
	
	@Override
	public AccessibleObject finalize(AccessibleObject o, ArrayEntry arrays, int[] marker) throws BaseException {
		while(arrays!=null) {
			mark(marker, arrays.offset);
			o.setField(fieldCodecs[arrays.offset].f, arrays.val);
			arrays=arrays.next;
		}
		if(marker[marker.length-1]<count) {
			int offset=count-marker[marker.length-1];
			Checker rover=checker;
			while(rover!=null&&offset>0) {
				if(isMarked(marker,rover.offset)) {
					if(rover.defaultValue!=null) {
						o.setField(getCodec(rover.number).f/*    fieldCodecs[rover.offset].f*/, rover.defaultValue);
						offset--;
					} else {
						throw new BaseException(BaseException.BAD_REQUEST, "Missing required value");
					}
				}
				rover=rover.next;
			}
			if(offset>0) {
				throw new BaseException(BaseException.BAD_REQUEST, "Cannot happen");
			}
		}
		return o;
	}
	
	private class Checker {
		int offset;
		int number;
		AccessibleObject defaultValue;
		Checker next;
		public Checker(Checker next, int offset, int number,AccessibleObject defaultValue) {
			super();
			this.number=number;
			this.next = next;
			this.offset = offset;
			this.defaultValue = defaultValue;
		}	
		
	}
}
