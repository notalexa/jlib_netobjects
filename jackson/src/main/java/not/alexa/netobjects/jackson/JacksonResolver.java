/*
 * Copyright (C) 2023 Not Alexa
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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;

import not.alexa.netobjects.Context;
import not.alexa.netobjects.api.CodingHint;
import not.alexa.netobjects.api.Helper;
import not.alexa.netobjects.api.ResolvableBy;
import not.alexa.netobjects.coding.AbstractTextCodingScheme;
import not.alexa.netobjects.coding.Codec;
import not.alexa.netobjects.coding.Codecs;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.coding.text.EnumCodec;
import not.alexa.netobjects.coding.xml.XMLCodingScheme;
import not.alexa.netobjects.types.ArrayTypeDefinition;
import not.alexa.netobjects.types.ArrayTypeDefinition.ArrayFlavour;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition.Builder;
import not.alexa.netobjects.types.ClassTypeDefinition.Builder.FieldBuilder;
import not.alexa.netobjects.types.Flavour;
import not.alexa.netobjects.types.JavaClass.Type;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.PrimitiveTypeDefinition;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.TypeLoader.LinkedLocal;
import not.alexa.netobjects.types.TypeResolver;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.AccessFactory;
import not.alexa.netobjects.types.access.Constructor;
import not.alexa.netobjects.types.access.Constructor.DeferredProvider;
import not.alexa.netobjects.utils.SimpleFieldMapper;
import not.alexa.netobjects.utils.TypeUtils;
import not.alexa.netobjects.utils.TypeUtils.ClassResolver;
import not.alexa.netobjects.utils.TypeUtils.ResolvedClass;


/**
 * Define a {@link TypeResolver} using Jackson Annotations. The following annotations are supported:
 * <ul>
 * <li>{@code @JsonProperty} including
 * <ul>
 * <li>{@code value()}: The field name
 * <li>{@code required()}: If set, the field is checked to be required
 * <li>{@code defaultValue()}: The default value is used if the field is omitted (and the field is omitted if it has value {@code defaultValue()}.
 * <br>If the field is of type {@code String}, the default value is modified since the defaults default value is the empty string. If the first character
 * is a zero character, the first character is omitted (it would be much better to define a default value which is (almost) never used like the zero character string
 * but that's not considered).
 * </ul>
 * Not implemented are
 * <ul>
 * <li>{@code namespace()}
 * <li>{@code index()} (but planned).
 * <li>{@code access()}
 * </ul>
 * <li>{@code @JsonRootName} The annotation is recognized but not evaluated. To define the root tag for an explicit type, use {@link XMLCodingScheme.Builder#setRootTag(String)}
 * and {@link XMLCodingScheme.Builder#setRootType(TypeDefinition)}. A more convenient API may be implemented in the future.
 * <li>{@code @JsonAutoDetect} (for fields only)
 * <li>{@code @JsonIgnore}
 * <li>{@code @JsonIgnoreType}
 * <li>{@code @JsonIgnoreProperties} is ignored but recognized as an annotation. Fields are always ignored in the underlying library.
 * <li>{@code @JsonCreator} (only constructor and without delegator support)
 * </ul>
 * Planned are to support the following annotations:
 * <ul>
 * <li>{@code @JacksonInject}
 * <li>{@code @JsonAnySetter}
 * <li>{@code @JsonSetter}
 * <li>{@code @JsonAnnyGBetter}
 * <li>{@code @JsonGetter}
 * <li>{@code @JsonPropertyOrder} (together with {@code JsonProperty.index()})
 * </ul>
 * Not planned are to support the following annotations:
 * <ul>
 * <li>{@code @JsonInclude} (this may be useful)
 * <li>{@code @JsonPropertyDescription}
 * <li>{@code @JsonFormat} (but serialization for <b>any</b> primitive type can be changed using the {@link AbstractTextCodingScheme.Builder#addCodec(Type, Codec)} method.
 * Therefore, the date format can be globally changed.
 * <li>{@code @JsonUnwrapped}
 * <li>{@code @JsonView}
 * <li>{@code @JsonEnumDefaultValue} (but the codec for a specific enum type can be set using {@link AbstractTextCodingScheme.Builder#addCodec(Type, Codec)}. This includes
 * a proper handling of unknown values.
 * <li>{@code @JsonRawValue} This contradicts the philosophy of a consistent data format. Helper classes should be used in this case.
 * <li>{@code @HJsonValue} This contradicts the philosophy of a consistent data format. Helper classes should be used in this case.
 * <li>All type handling annotations.
 * <li>All object reference annotations (But {@code JsonIdentityInfo} may be used as an annotation of the type to indicate backreference support for this type
 * <li>All meta annotations
 * </ul>
 * The type resolver should not create a type definition for all types. Therefore a mechanism is provided to select types which defines type definition through this
 * resolver. The resolver considers the type as legitim if:
 * <ul>
 * <li>a jackson annotation is present while resolving
 * <li>a resource with the type name (dots are replaced with slashes) and extension {@code .jackson} is present
 * <li>an annotation {@link ResolvableBy} with name {@code jackson} is present on the type (usually an extension of the original class)
 * <li>an annoation {@link ResolvableBy} with name {@code jackson} is present after {@code extends}. This doesn't work in all cases (Android for example) and should
 * be handled with care.
 * </ul>
 * The last two conditions are almost equivalent. The only difference is that in the first case a non trivial constructor is resolved in the annotated class, in the
 * second case in the (annotated) superclass. In both cases, the type is internally handled as the extension of the original POJO and not the POJO itself. This
 * typically doesn't matter if communication is done using an explicit coding scheme (where no type information are included).
 *  
 * @author notalexa
 * @see TypeResolver
 */
public class JacksonResolver implements TypeResolver {
	Codecs codecs;
	Context context;

    public JacksonResolver() {
    	codecs=Codecs.defaultTextCodecs();
    }

    @Override
    public TypeDefinition resolve(LoaderIntermediate loader, ObjectType type) {
        if(type instanceof Type) try {
            Type t=(Type)type;
            boolean explicitlyDefined=false;
            LinkedLocal linkedClass=t.asLinkedLocal(loader.getClassLoader());
            Class<?> clazz=linkedClass.asClass();
            Class<?> constructorClazz=clazz;
            ResolvableBy hint=clazz.getAnnotation(ResolvableBy.class);
            explicitlyDefined|=(hint!=null&&hint.value().equals("jackson"))||loader.getClassLoader().getResource(clazz.getName().replace('.','/')+".jackson")!=null;
            if(!explicitlyDefined) try {
            	hint=clazz.getAnnotatedSuperclass().getAnnotation(ResolvableBy.class);
                explicitlyDefined|=(hint!=null&&hint.value().equals("jackson"));
                if(explicitlyDefined) {
                	constructorClazz=clazz.getSuperclass();
                }
            } catch(Throwable t0) {
            }
            List<String> constructorFields=new ArrayList<>();
            defineConstructorFields(constructorClazz,false,constructorFields);
            RuntimeInfos infos=new RuntimeInfos(constructorClazz);
            ClassTypeDefinition result=defineFromClazz(loader,type,clazz,infos);
            if(result!=null&&(explicitlyDefined||infos.annotationSeen)) {
	            if(constructorFields.size()>0||infos.fieldMap.size()>0) {
	            	loader.addProvider(type,new DeferredProvider(clazz,constructorFields,infos.fieldMap.size()>0?new SimpleFieldMapper(infos.fieldMap):Constructor.FieldMapper.IDENTITY) {
						@Override
						public java.lang.reflect.Constructor<?> findConstructor(Class<?> enclosingClass, Class<?> clazz) throws Throwable {
							return defineConstructorFields(clazz, true,constructorFields);
						}
	            	});
	            }
	            return result;
            }
        } catch(Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    private java.lang.reflect.Constructor<?> defineConstructorFields(Class<?> clazz, boolean resolveOnly,List<String> constructorFields) {
    	int offset=Constructor.getEnclosingClass(clazz)==null?0:1;
    	outerloop: for(java.lang.reflect.Constructor<?> c:clazz.getDeclaredConstructors()) {
    		if(c.getAnnotation(JsonCreator.class)!=null||(resolveOnly&&c.getParameterCount()==constructorFields.size()+offset)) {
    			int parameterCount=0;
    			for(Parameter p:c.getParameters()) {
    				if(parameterCount<offset) {
    					continue;
    				}
    				JsonProperty props=p.getAnnotation(JsonProperty.class);
    				if(props==null) {
    					if(!resolveOnly) {
    						constructorFields.clear();
    					}
    					continue outerloop;
    				}
    				if(props.value().length()>0) {
    					if(resolveOnly) {
    						if(!constructorFields.get(parameterCount-offset).equals(props.value())) {
    							continue outerloop;
    						}
    					} else {
    						constructorFields.add(props.value());
    					}
    				} else if(p.isNamePresent()) {
    					if(resolveOnly) {
    						if(!constructorFields.get(parameterCount-offset).equals(p.getName())) {
    							continue outerloop;
    						}
    					} else {
    						constructorFields.add(p.getName());
    					}
    				} else {
    					if(!resolveOnly) {
    						constructorFields.clear();
    					}
    					continue outerloop;
    				}
    				parameterCount++;
    			}
    			c.setAccessible(true);
    			return c;
    		}
    	}
    	return null;
	}

	private ClassTypeDefinition defineFromClazz(LoaderIntermediate loader,ObjectType type,Class<?> clazz,RuntimeInfos infos) {
        ClassResolver classResolver=TypeUtils.createClassResolver(clazz);
        ClassTypeDefinition def=new ClassTypeDefinition(clazz);
        loader.register(type,def);
        Builder builder=def.createBuilder();
        return defineFromClazz0(loader,builder,classResolver,clazz,infos).build();
    }
    
    private Builder defineFromClazz0(LoaderIntermediate loader,Builder builder,ClassResolver resolver,Class<?> clazz,RuntimeInfos infos) {
        if(Object.class.equals(clazz)) {
            return builder;
        }
        defineFromClazz0(loader,builder,resolver,clazz.getSuperclass(),infos);
        for(Field f:clazz.getDeclaredFields()) {
            JsonProperty prop=f.getAnnotation(JsonProperty.class);
            String name=f.getName();
            String defaultValue=null;
            int number=-1;
            boolean required=false;
            if(prop!=null) {
            	infos.annotationSeen=true;
            	number=prop.index();
                String n=prop.value().length()>0?prop.value():name;
                if(!n.equals(name)) {
                	infos.fieldMap.put(clazz.getName()+"#"+n,name);
                	name=n;
                }
                if(prop.defaultValue().length()>0) {
                	defaultValue=prop.defaultValue();
                	if(f.getType().equals(String.class)&&defaultValue.charAt(0)==0) {
                		defaultValue=defaultValue.substring(1);
                	}
                }
                required=prop.required();
            } else if(infos.skip(f)) {
            	continue;
            }
            ResolvedClass fieldClass=resolver.resolve(f);
            TypeDefinition type=resolveType(loader,resolver,fieldClass,infos);
            if(type!=null) {
            	Object d=null;
            	if(defaultValue!=null) try {
            		Buffer buffer=new Buffer(defaultValue);
            		Access access=buffer.resolve(type);
            		if(access!=null) {
            			Codec codec=codecs.get(access);
            			if(codec==null&&type.getFlavour()==Flavour.EnumType) {
            				codecs.put(access, codec=new EnumCodec(type.getJavaClassType()));
            			}
            			d=codec==null?null:codec.decode(buffer);
            		}
            	} catch(Throwable t) {
            	}
                enrich(fieldClass,builder.createField(name, type)).setOptional(!required)
                		.setAbstract(false)
                		.setNumber(number)
                		.setDefaultValue(d).build();
            }
        }
        return builder;
    }

    private String getName(AnnotatedElement e,String defaultValue) {
        for(not.alexa.netobjects.api.Field f:Helper.getFields(e)) {
	        if("*".equals(f.type())&&f.name().length()>0) {
	            return f.name();
	        }
        }
        return defaultValue;
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
    
    private TypeDefinition resolveType(LoaderIntermediate loader,ClassResolver resolver,ResolvedClass clazz,RuntimeInfos infos) {
        if(clazz.isArray()) {
            ResolvedClass[] parameters=clazz.getParameters();
            if(parameters.length==1) {
            	if(clazz.getResolvedClass().equals(byte[].class)) {
            		return PrimitiveTypeDefinition.getTypeDescription(byte[].class);
            	} else {
            		TypeDefinition componentType=resolveType(loader,resolver,parameters[0],infos);
            		return componentType==null?null:new ArrayTypeDefinition(resolveType(loader,resolver,parameters[0],infos));
            	}
            } else if(parameters.length==2) {
                String keyName=getName(parameters[0],"key");
                String valueName=getName(parameters[1],"value");
                TypeDefinition key=resolveType(loader,resolver,parameters[0],infos);
                TypeDefinition value=resolveType(loader,resolver,parameters[1],infos);
                if(key!=null&&value!=null) {
                    return new ArrayTypeDefinition(ArrayFlavour.Map,
                    		enrich(parameters[1],enrich(parameters[0],new ClassTypeDefinition().createBuilder()
                            .createField(keyName, key)).build().createField(valueName, value)).build().build()
                            );
                }
                return null;
            } else {
                throw new RuntimeException();
            }
        } else if(clazz.hasParameters()) {
            ClassTypeDefinition def=new ClassTypeDefinition();
            return defineFromClazz0(loader, def.createBuilder(), resolver, clazz.getResolvedClass(),infos).build();
        } else {
            return loader.resolveType(ObjectType.createClassType(clazz.getResolvedClass()));
        }
    }
    
    
    public class Buffer implements Decoder.Buffer {
    	private String text;
		public Buffer(String text) {
			this.text=text;
		}

		@Override
		public Context getContext() {
			if(context==null) {
				synchronized (JacksonResolver.this) {
					if(context==null) {
						context=Context.createRootContext();
					}
				}
			}
			return context;
		}

		@Override
		public Constructor resolve(Context context, Type type) {
			return null;
		}

		private Access resolve(TypeDefinition type) {
			return resolve(getContext(),type);
		}

		@Override
		public Access resolve(Context context, TypeDefinition type) {
			switch(type.getFlavour()) {
				case PrimitiveType:
				case EnumType:return AccessFactory.getDefault().resolve(context, type);
				default: return null;
			}
		}

		@Override
		public Access resolve(Access referrer, TypeDefinition type) {
			return resolve(context,type);
		}

		@Override
		public byte[] getByteContent() {
			return text.getBytes();
		}

		@Override
		public CharSequence getCharContent() {
			return text;
		}

		@Override
		public <T> T castTo(Context context, Class<T> clazz) {
			return context.castTo(clazz);
		}
    }
    
    private class RuntimeInfos {
    	Class<?> topClass;
    	boolean annotationSeen;
    	Map<String,String> fieldMap=new HashMap<>();
    	RuntimeInfos(Class<?> topClass) {
    		this.topClass=topClass;
    		annotationSeen=topClass.isAnnotationPresent(JsonRootName.class)||topClass.isAnnotationPresent(JsonIgnoreProperties.class);
    	}
    	
    	private JsonAutoDetect.Visibility getVisibility(Class<?> clazz) {
			JsonAutoDetect autoDetect=clazz.getAnnotation(JsonAutoDetect.class);
			if(autoDetect!=null) {
				annotationSeen|=clazz.equals(topClass);
				if(autoDetect.fieldVisibility()!=Visibility.DEFAULT) {
					return autoDetect.fieldVisibility();
				}
			}
			if(clazz.getSuperclass()!=null) {
				return getVisibility(clazz.getSuperclass());
			}
			return Visibility.PUBLIC_ONLY;
    	}
    	
		public boolean skip(Field f) {
			if(f.getAnnotation(JsonIgnore.class)!=null||f.getType().getAnnotation(JsonIgnoreType.class)!=null) {
				annotationSeen=true;
				return true;
			} else if(!Modifier.isStatic(f.getModifiers())) {
				JsonAutoDetect.Visibility visibility=getVisibility(f.getDeclaringClass());
				return !visibility.isVisible(f);
			} else {
				return true;
			}
		}
    }
}
