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
package not.alexa.netobjects.jackson;

import java.lang.reflect.AnnotatedElement;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.netobjects.api.CodingHint;
import not.alexa.netobjects.api.Helper;
import not.alexa.netobjects.coding.Codec;
import not.alexa.netobjects.coding.text.EnumCodec;
import not.alexa.netobjects.jackson.JacksonResolver.Buffer;
import not.alexa.netobjects.jackson.JacksonResolver.RuntimeInfos;
import not.alexa.netobjects.types.ClassTypeDefinition.Builder;
import not.alexa.netobjects.types.ClassTypeDefinition.Builder.FieldBuilder;
import not.alexa.netobjects.types.Flavour;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.ClassAccessInfo;
import not.alexa.netobjects.utils.TypeUtils.ResolvedClass;

/**
 * Class representing one field.
 */
class JacksonField implements Comparable<JacksonField> {
	protected String name;
	protected String fieldName;
	protected int id;
	protected int sortId;
	int prio;
	protected AnnotatedElement e;
	protected TypeDefinition type;
	protected ResolvedClass fieldClass;
	protected Class<?> declaringClass;
	protected ClassAccessInfo.FieldAccessInfo access;

	public JacksonField(String name,String fieldName,int id,Class<?> declaringClass,int prio,AnnotatedElement e,ResolvedClass fieldClass,TypeDefinition type) {
		this.name=name;
		this.fieldName=fieldName;
		this.id=this.sortId=id;
		this.e=e;
		this.prio=prio;
		this.type=type;
		this.declaringClass=declaringClass;
		this.fieldClass=fieldClass;
		if(e.isAnnotationPresent(JsonProperty.class)&&e.getAnnotation(JsonProperty.class).index()>=0) {
			sortId=e.getAnnotation(JsonProperty.class).index();
		}
	}
	
	public void add(ClassAccessInfo.FieldAccessInfo access) {
		this.access=access.merge(this.access);
	}
	
    protected FieldBuilder enrich(ResolvedClass fieldClass,FieldBuilder builder) {
        for(not.alexa.netobjects.api.Field f:Helper.getFields(fieldClass)) {
	        if(!"*".equals(f.type())&&f.name().length()>0) {
	            builder.addTag(f.type(),f.name());
	        }
        }
        for(CodingHint hint:Helper.getCodingHints(fieldClass)) {
        	builder.addHint(hint.value());
        }
    	return builder;
    }


	void add(Builder builder,RuntimeInfos infos) {
        String defaultValue=null;
        int number=-1;
        boolean required=false;
        JsonProperty prop=e.getAnnotation(JsonProperty.class);
        if(!fieldName.equals(name)) {
        	infos.fieldMap.put(declaringClass.getName()+"#"+name,fieldName);
        }
        if(prop!=null) {
        	number=prop.index();
            required=prop.required();
            if(prop.defaultValue().length()>0) {
            	defaultValue=prop.defaultValue();
            	if(fieldClass.getResolvedClass().equals(String.class)&&defaultValue.charAt(0)==0) {
            		defaultValue=defaultValue.substring(1);
            	}
            }
        }
        if(type!=null) {
        	Object d=null;
        	if(defaultValue!=null) try {
        		Buffer buffer=infos.createBuffer(defaultValue);
        		Access access=buffer.resolve(type);
        		if(access!=null) {
        			Codec codec=infos.getCodec(access);
        			if(codec==null&&type.getFlavour()==Flavour.EnumType) {
        				infos.addCodec(access, codec=new EnumCodec(type.getJavaClassType()));
        			}
        			d=codec==null?null:codec.decode(buffer);
        		}
        	} catch(Throwable t) {
        	}
            FieldBuilder fieldBuilder= enrich(fieldClass,builder.createField(name, type)).setOptional(!required)
            		.setAbstract(false)
            		.setNumber(number)
            		.setDefaultValue(d);
            JsonAlias alias=e.getAnnotation(JsonAlias.class);
            if(alias!=null&&alias.value().length>0) {
            	fieldBuilder.addTag("json:alt", String.join(",",alias.value()));
            }
            fieldBuilder.build();
        }
	}

	@Override
	public int compareTo(JacksonField o) {
		return sortId-o.sortId;
	}
}
