/*
 * Copyright (C) 2020 Not Alexa
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
package not.alexa.netobjects.types;

import not.alexa.netobjects.types.ArrayTypeDefinition.ArrayFlavour;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.EnumTypeDefinition.Value;

/**
 * Type definitions of all types in this package. Because
 * of initialization race conditions defined in it's own
 * class.
 * 
 * @author notalexa
 *
 */
class Types {
	static TypeDefinition TYPE=new InterfaceTypeDefinition(TypeDefinition.class);
	
	static final ClassTypeDefinition DEFERRED_TYPE=new ClassTypeDefinition(DeferredObject.class)
			.createBuilder()
			.build();

	static final ClassTypeDefinition UNKNOWN_TYPE=new ClassTypeDefinition(UnknownTypeDefinition.class)
			.createBuilder()
				.setEnableObjectRefs(true)
				.createField("types",new ArrayTypeDefinition(PrimitiveTypeDefinition.getTypeDescription(ObjectType.class)))
				    .setOptional(true)
					.addTag("XML","type").build()
				.build();
	
	static final ClassTypeDefinition PRIMITIVE_TYPE=new ClassTypeDefinition(PrimitiveTypeDefinition.class)
			.createBuilder()
				.setEnableObjectRefs(true)
				.createField("types",new ArrayTypeDefinition(PrimitiveTypeDefinition.getTypeDescription(ObjectType.class)))
					.addTag("XML","type").build()
				.build();
	static final ClassTypeDefinition VALUE_TYPE=new ClassTypeDefinition(Value.class)
			.createBuilder()
				.createField("index",PrimitiveTypeDefinition.getTypeDescription(Integer.TYPE))
					.addTag("XML","@index").build()
				.createField("value",PrimitiveTypeDefinition.getTypeDescription(String.class))
					.addTag("XML","#text").build()
				.build();
	static final ClassTypeDefinition ENUM_TYPE=new ClassTypeDefinition(EnumTypeDefinition.class)
			.createBuilder()
				.setEnableObjectRefs(true)
				.createField("types",new ArrayTypeDefinition(PrimitiveTypeDefinition.getTypeDescription(ObjectType.class)))
				    .setOptional(true)
					.addTag("XML","type").build()
				.addField("values",new ArrayTypeDefinition(VALUE_TYPE))
				.build();
	static final ClassTypeDefinition ARRAY_TYPE=new ClassTypeDefinition(ArrayTypeDefinition.class)
			.createBuilder()
				.setEnableObjectRefs(true)
				.createField("types",new ArrayTypeDefinition(PrimitiveTypeDefinition.getTypeDescription(ObjectType.class)))
				    .setOptional(true)
					.addTag("XML","type").build()
			    .createField("flavour",new EnumTypeDefinition(ArrayFlavour.class)).setDefaultValue(ArrayFlavour.Array).build()
				.addField("component",TYPE)
				.build();
	static final ClassTypeDefinition METHOD_TYPE=new ClassTypeDefinition(MethodTypeDefinition.class)
			.createBuilder()
				.setEnableObjectRefs(true)
				.createField("types",new ArrayTypeDefinition(PrimitiveTypeDefinition.getTypeDescription(ObjectType.class)))
                    .setOptional(true)
                    .addTag("XML","type").build()
				.createField("name",PrimitiveTypeDefinition.getTypeDescription(String.class))
					.addTag("XML","@name").build()
				.createField("parameterTypes",new ArrayTypeDefinition(TYPE))
				    .setOptional(true)
					.addTag("XML","parameterType").build()
				.createField("returnTypes",new ArrayTypeDefinition(TYPE))
				    .setOptional(true)
					.addTag("XML","returnType").build()
				.build();
	public static final ClassTypeDefinition INTERFACE_TYPE=new ClassTypeDefinition(InterfaceTypeDefinition.class)
			.createBuilder()
				.setEnableObjectRefs(true)
				.createField("types",new ArrayTypeDefinition(PrimitiveTypeDefinition.getTypeDescription(ObjectType.class)))
				    .setOptional(true)
					.addTag("XML","type").build()
				.createField("implementors",new ArrayTypeDefinition(PrimitiveTypeDefinition.getTypeDescription(ObjectType.class)))
				    .setOptional(true)
					.addTag("XML","implementor").build()
				.createField("methods",new ArrayTypeDefinition(METHOD_TYPE))
				    .setOptional(true)
					.addTag("XML","method").build()
				.build();

	static final ClassTypeDefinition FIELD_TYPE=new ClassTypeDefinition(Field.class)
			.createBuilder()
				.setEnableObjectRefs(true)
				.createField("number",PrimitiveTypeDefinition.getTypeDescription(Integer.TYPE))
					.addTag("XML","@number").build()
				.createField("name",PrimitiveTypeDefinition.getTypeDescription(String.class))
					.addTag("XML","@name").build()
	            .createField("abstract",PrimitiveTypeDefinition.getTypeDescription(Boolean.class))
                    .setDefaultValue(false)
                    .addTag("XML","@abstract").build()
	            .createField("optional",PrimitiveTypeDefinition.getTypeDescription(Boolean.class))
	                .setDefaultValue(false)
                    .addTag("XML","@optional").build()
				.addField("type",TYPE)
				.createField("tags",new ArrayTypeDefinition(ArrayFlavour.Map,new ClassTypeDefinition()
						.createBuilder()
						.createField("schema",PrimitiveTypeDefinition.getTypeDescription(String.class))
							.addTag("XML","@schema").build()
						.createField("name",PrimitiveTypeDefinition.getTypeDescription(String.class))
							.addTag("XML","@name").build()
						.build()))
				    .setOptional(true)
					.addTag("XML","tag").build()
				.createField("hints",new ArrayTypeDefinition(PrimitiveTypeDefinition.getTypeDescription(String.class)))
				    .setOptional(true)
					.addTag("XML","hint").build()
	            .createField("default",PrimitiveTypeDefinition.getTypeDescription(Object.class))
	                .setOptional(true)
	                .build()
				.build();
	static final ClassTypeDefinition CLASS_TYPE=new ClassTypeDefinition(ClassTypeDefinition.class)
			.createBuilder()
				.setEnableObjectRefs(true)
				.createField("types",new ArrayTypeDefinition(PrimitiveTypeDefinition.getTypeDescription(ObjectType.class)))
                    .setOptional(true)
                    .addTag("XML","type").build()
				.createField("enableObjectRefs",PrimitiveTypeDefinition.getTypeDescription(Boolean.TYPE))
                    .setDefaultValue(false)
                    .addTag("XML","@enableObjectRefs").build()
	            .createField("abstract",PrimitiveTypeDefinition.getTypeDescription(Boolean.TYPE))
                    .setDefaultValue(false)
                    .addTag("XML","@abstract").build()
				.createField("fields",new ArrayTypeDefinition(FIELD_TYPE))
				    .setOptional(true)
					.addTag("XML","field").build()
				.createField("methods",new ArrayTypeDefinition(METHOD_TYPE))
				    .setOptional(true)
					.addTag("XML","method").build()
				.build();
	
	static final ClassTypeDefinition LAMBDA=new ClassTypeDefinition(Lambda.class)
	        .createBuilder()
	            .addField("method", PrimitiveTypeDefinition.getTypeDescription(ObjectType.class))
                .createField("self", PrimitiveTypeDefinition.getTypeDescription(Object.class))
                    .setOptional(true)
                    .build()
                .createField("args", new ArrayTypeDefinition(PrimitiveTypeDefinition.getTypeDescription(Object.class)))
                    .setOptional(true)
                    .build()
                .build();

	private Types() {
	}
}
