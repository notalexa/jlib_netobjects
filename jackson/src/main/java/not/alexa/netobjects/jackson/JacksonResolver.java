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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonSetter;

import not.alexa.netobjects.Context;
import not.alexa.netobjects.api.CodingHint;
import not.alexa.netobjects.api.Helper;
import not.alexa.netobjects.api.ResolvableBy;
import not.alexa.netobjects.coding.AbstractTextCodingScheme;
import not.alexa.netobjects.coding.Codec;
import not.alexa.netobjects.coding.Codecs;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.coding.xml.XMLCodingScheme;
import not.alexa.netobjects.types.ArrayTypeDefinition;
import not.alexa.netobjects.types.ArrayTypeDefinition.ArrayFlavour;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition.Builder;
import not.alexa.netobjects.types.ClassTypeDefinition.Builder.FieldBuilder;
import not.alexa.netobjects.types.JavaClass.Type;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.PrimitiveTypeDefinition;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.TypeLoader.LinkedLocal;
import not.alexa.netobjects.types.TypeResolver;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.AccessContext;
import not.alexa.netobjects.types.access.AccessFactory;
import not.alexa.netobjects.types.access.ClassAccessInfo;
import not.alexa.netobjects.types.access.RuntimeInfo;
import not.alexa.netobjects.types.access.RuntimeInfo.InjectorInfos;
import not.alexa.netobjects.types.access.ClassAccessInfo.FieldAccessInfo;
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
 * <li>{@code @JsonAutoDetect}
 * <li>{@code @JsonSetter}
 * <li>{@code @JsonGetter}
 * <li>{@code @JsonIgnore}
 * <li>{@code @JsonIgnoreType}
 * <li>{@code @JsonIgnoreProperties}
 * <li>{@code @JsonIncludeProperties}
 * <li>{@code @JsonAlias}
 * <li>{@code @JsonCreator} (only constructor and without delegator support)
 * <li>{@code @JsonPropertyOrder} (together with {@code JsonProperty.index()})
 * <li>{@code @JacksonInject}
 * </ul>

 * Not planned are to support the following annotations:
 * <ul>
 * <li>{@code @JsonAnySetter}
 * <li>{@code @JsonAnyGetter}
 * <li>{@code @JsonInclude} (this may be useful)
 * <li>{@code @JsonPropertyDescription}
 * <li>{@code @JsonFormat} (but serialization for <b>any</b> primitive type can be changed using the {@link AbstractTextCodingScheme.Builder#addCodec(Type, Codec)} method.
 * Therefore, the date format can be globally changed.
 * <li>{@code @JsonUnwrapped}
 * <li>{@code @JsonView}
 * <li>{@code @JsonEnumDefaultValue} (but the codec for a specific enum type can be set using {@link AbstractTextCodingScheme.Builder#addCodec(Type, Codec)}. This includes
 * a proper handling of unknown values.
 * <li>{@code @JsonRawValue} This contradicts the philosophy of a consistent data format. Helper classes should be used in this case.
 * <li>{@code @JsonValue} This contradicts the philosophy of a consistent data format. Helper classes should be used in this case.
 * <li>All type handling annotations.
 * <li>All object reference annotations (but {@code JsonIdentityInfo} may be used as an annotation of the type to indicate backreference support for this type
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
            Class<?> clazz=linkedClass==null?null:linkedClass.asClass();
            if(clazz!=null) {
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
	            RuntimeInfos infos=new RuntimeInfos(type,constructorClazz);
	            ClassTypeDefinition def=new ClassTypeDefinition(clazz);
	            loader.register(type,def);
	            return defineFromClazz(loader,TypeUtils.createClassResolver(clazz),explicitlyDefined,def,infos);
            }
        } catch(Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    private ClassAccessInfo defineConstructorFields(Class<?> enclosingClass,Class<?> clazz, RuntimeInfos infos) {
    	int offset=enclosingClass==null?0:1;
    	int no=-1;
    	int defaultConstructor=-1;
    	outerloop: for(java.lang.reflect.Constructor<?> c:clazz.getDeclaredConstructors()) {
    		no++;
    		if(infos.isVisible(c)) {
    			int parameterCount=0;
    			int parameterIndex=0;
    			InjectorInfos injectorInfos=new InjectorInfos();
    			for(Parameter p:c.getParameters()) {
    				if(parameterCount<offset) {
    					continue;
    				}
    				JacksonInject inject=p.getAnnotation(JacksonInject.class);
    				if(inject!=null) {
    					infos.annotationSeen=true;
    					injectorInfos.addParameter(clazz, parameterIndex,inject.value());
    				} else {
	    				JsonProperty props=p.getAnnotation(JsonProperty.class);
	    				if(props==null||props.value().length()==0) {
	    					continue outerloop;
	    				} else {
	    					infos.annotationSeen=true;
	    				}
	    				parameterCount++;
    				}
    			}
    			infos.annotationSeen=true;
    			return new ClassAccessInfo(clazz, no,injectorInfos);
    		} else if(c.getParameterCount()==offset) {
    			defaultConstructor=no;
    		}
    	}
    	return new ClassAccessInfo(clazz, defaultConstructor,null);
	}

	private ClassTypeDefinition defineFromClazz(LoaderIntermediate loader, ClassResolver classResolver,boolean explicitlyDefined,ClassTypeDefinition def,RuntimeInfos infos) {
        Class<?> enclosingClass=RuntimeInfo.getEnclosingClass(infos.topClass);
        List<String> constructorFields=new ArrayList<>();
        ClassAccessInfo constructor=defineConstructorFields(enclosingClass,infos.topClass,infos);
        Builder builder=def.createBuilder();
        if(constructor.hasConstructorInfos()) for(Parameter p:constructor.get(enclosingClass,infos.topClass).getParameters()) {
        	if(enclosingClass==null) {
        		if(p.getAnnotation(JacksonInject.class)==null) {
		        	JacksonField f=add(loader,classResolver,infos,null,classResolver.getRootClass(),classResolver.resolve(p),Integer.MAX_VALUE,p);
		        	constructorFields.add(f.name);
        		}
        	} else {
        		enclosingClass=null;
        	}
        }
        defineFromClazz0(loader,classResolver,classResolver.getRootClass(),infos);
        if(explicitlyDefined||infos.annotationSeen) {
	        List<JacksonField> fields=infos.fields.stream().filter(f->!f.isReadOnly()).collect(Collectors.toList());
	        String[] order=classResolver.getRootClass().isAnnotationPresent(JsonPropertyOrder.class)?classResolver.getRootClass().getAnnotation(JsonPropertyOrder.class).value():null;
	        if(order!=null) {
	        	Map<String,Integer> orderMap=new HashMap<String, Integer>();
	        	for(int i=0;i<order.length;i++) {
	        		orderMap.put(order[i],i);
	        	}
	        	for(JacksonField f:infos.fields) {
	        		if(orderMap.containsKey(f.name)) {
	        			f.sortId=orderMap.get(f.name);
	        		} else {
	        			f.sortId+=orderMap.size();
	        		}
	        	}
	        }
	        Collections.sort(fields);
	        ClassAccessInfo.FieldAccessInfo[] fieldAccess=new ClassAccessInfo.FieldAccessInfo[fields.size()];
	        int c=0;
	        for(JacksonField field:fields) {
	        	field.add(builder,infos);
	        	fieldAccess[c++]=field.access;
	        }
	        ClassTypeDefinition typeDefinition=builder.build();
        	loader.addProvider(infos.type,constructor.forFieldAccess(fieldAccess).getProvider(classResolver.getRootClass(),constructorFields,infos.injectorInfos,  infos.fieldMap.size()>0?new SimpleFieldMapper(infos.fieldMap):RuntimeInfo.FieldMapper.IDENTITY));
	        return typeDefinition;
        } else {
        	return null;
        }
    }
	
	private JacksonField add(LoaderIntermediate loader,ClassResolver resolver,RuntimeInfos infos,String name,Class<?> clazz,ResolvedClass fieldClass,int prio,AnnotatedElement e) {
        String fieldName=name;
        JsonProperty prop=e.getAnnotation(JsonProperty.class);
        if(prop!=null) {
        	infos.annotationSeen=true;
           	name=prop.value().length()>0?prop.value():name;
           	prio=Math.max(prio, prio+0x100);
        }
        if(name!=null) { 
        	if(infos.declaredFields.containsKey(name)) {
        		JacksonField f=infos.fields.get(infos.declaredFields.get(name));
        		if(f.prio<prio) {
           			FieldAccessInfo accessInfo=null;
           			TypeDefinition type=f.type;
           			if(f.fieldClass.equals(fieldClass)) {
           				accessInfo=f.access;
           			} else {
           				type=resolveType(loader,resolver,fieldClass,infos);
           				if(type==null) {
           					return null;
           				}
           			}
       	            f=new JacksonField(name,fieldName==null?name:fieldName, f.id, clazz, prio,e, fieldClass, type);
       	            f.access=accessInfo;
           			infos.fields.set(f.id, f);
        			return f;
        		} else {
        			return f;
        		}
        	}
	        TypeDefinition type=resolveType(loader,resolver,fieldClass,infos);
	        if(type!=null) {
	            infos.declaredFields.put(name,infos.fields.size());
	            JacksonField f=new JacksonField(name,fieldName==null?name:fieldName, infos.fields.size(), clazz, prio,e, fieldClass, type);
	            infos.fields.add(f);
	            return f;
	        }
        }
        return null;
	}
	
	private String resolveGetter(Method m,RuntimeInfos infos) {
		if(RuntimeInfo.methodPrio(0,m)>0) {
			JsonGetter getter=m.getAnnotation(JsonGetter.class);
			if(getter!=null&&getter.value().length()>0) {
				return getter.value();
			} else {
				JsonProperty prop=m.getAnnotation(JsonProperty.class);
				if(prop!=null&&prop.value().length()>0) {
					return prop.value();
				}
			}
			if(m.getName().startsWith("get")&&m.getName().length()>3&&infos.isVisible(VisibilityType.Getter, m)) {
				return VisibilityType.Getter.toName(m.getName());
			} else	if(m.getParameterCount()==0&&m.getName().startsWith("is")&&m.getName().length()>2&&Boolean.TYPE.equals(m.getReturnType())&&infos.isVisible(VisibilityType.IsGetter, m)) {
				return VisibilityType.IsGetter.toName(m.getName());
			}
		}
		return null;
	}
	
	private Method resolveSetter(String name,java.lang.reflect.Type valueType,RuntimeInfos infos) {
		return resolveSetter(name, infos.topClass, valueType, infos);
	}
	
	private Method priorize(Method m1,Method m2) {
		return m2==null||RuntimeInfo.methodPrio(1,m1)>=RuntimeInfo.methodPrio(1,m2)?m1:m2;
	}
	
	private Method resolveSetter(String name,Class<?> clazz,java.lang.reflect.Type valueType,RuntimeInfos infos) {
		if(clazz!=null) {
			Method candidate=null;
			for(Method m:clazz.getDeclaredMethods()) {
				if((m.getGenericParameterTypes().length==1&&m.getGenericParameterTypes()[0].equals(valueType))
						||(m.getGenericParameterTypes().length==2&&(Context.class.isAssignableFrom(m.getParameterTypes()[0])||AccessContext.class.isAssignableFrom(m.getParameterTypes()[0]))&&m.getGenericParameterTypes()[1].equals(valueType))) {
					JsonSetter setter=m.getAnnotation(JsonSetter.class);
					if(setter!=null&&setter.value().equals(name)) {
						candidate=priorize(m,candidate);
					} else {
						JsonProperty prop=m.getAnnotation(JsonProperty.class);
						if((prop!=null&&prop.value().equals(name))) {
							candidate=priorize(m,candidate);
						} else if(name.equals(VisibilityType.Setter.toName(m.getName()))&&infos.isVisible(VisibilityType.Setter,m)) {
							candidate=priorize(m,candidate);
						}
					}
				}
			}
			if(candidate!=null) {
				return candidate;
			} else {
				return resolveSetter(name,clazz.getSuperclass(),valueType,infos);
			}
		} else {
			return null;
		}
	}
    
    private void defineFromClazz0(LoaderIntermediate loader,ClassResolver resolver,Class<?> clazz,RuntimeInfos infos) {
        if(clazz==null||Object.class.equals(clazz)) {
            return;
        }
        defineFromClazz0(loader,resolver,clazz.getSuperclass(),infos);
        int j=-1;
        for(Method m:clazz.getDeclaredMethods()) {
        	j++;
            JacksonInject inject=m.getAnnotation(JacksonInject.class);
            if(inject!=null) {
            	infos.injectorInfos.addMethod(clazz, j, inject.value());
            } else {
	        	String name=resolveGetter(m,infos);
	        	if(name!=null) {
	        		JacksonField field=add(loader, resolver, infos, name, clazz, resolver.resolve(m),RuntimeInfo.methodPrio(0,m),m);
	        		if(field!=null) {
	        			Method setter=resolveSetter(name,m.getGenericReturnType(), infos);
	        			field.add(new ClassAccessInfo.FieldAccessInfo(new ClassAccessInfo.AccessRef(m),setter==null?null:new ClassAccessInfo.AccessRef(setter)));
	        		}
	        	}
            }
        }
        int i=-1;
        for(Field f:clazz.getDeclaredFields()) {
        	i++;
            if(!infos.isVisible(f)) {
            	continue;
            }
            JacksonInject inject=f.getAnnotation(JacksonInject.class);
            if(inject!=null) {
            	infos.injectorInfos.addField(clazz, i, inject.value());
            } else {
	            JacksonField field=add(loader,resolver,infos,f.getName(),clazz,resolver.resolve(f),0,f);
	            if(field!=null) {
	            	Method setter=resolveSetter(field.name,f.getGenericType(), infos);
	            	ClassAccessInfo.AccessRef fieldRef=new ClassAccessInfo.AccessRef(f);
	    			field.add(new ClassAccessInfo.FieldAccessInfo(fieldRef,setter==null?fieldRef:new ClassAccessInfo.AccessRef(setter)));
	            }
            }
        }
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
            		return componentType==null?null:new ArrayTypeDefinition(componentType);
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
            return defineFromClazz(loader, clazz.asResolver(), true, new ClassTypeDefinition(),new RuntimeInfos(infos.type,clazz.getResolvedClass()));
        } else {
            return loader.resolveType(ObjectType.createClassType(clazz.getCodingClass()));
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
		public RuntimeInfo resolve(Context context, Type type) {
			return null;
		}

		Access resolve(TypeDefinition type) {
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
    
    public class RuntimeInfos {
    	ObjectType type;
    	Class<?> topClass;
    	boolean annotationSeen;
    	InjectorInfos injectorInfos=new InjectorInfos();
    	Map<String,String> fieldMap=new HashMap<>();
    	Map<String,Integer> declaredFields=new HashMap<>();
    	List<JacksonField> fields=new ArrayList<>();
    	Set<String> ignoreProperties;
    	Set<String> includeProperties;
    	RuntimeInfos(ObjectType type,Class<?> topClass) {
    		this.type=type;
    		this.topClass=topClass;
    		annotationSeen=topClass.isAnnotationPresent(JsonRootName.class)
    				||topClass.isAnnotationPresent(JsonIgnoreProperties.class)
    				||topClass.isAnnotationPresent(JsonIncludeProperties.class)
    				||topClass.isAnnotationPresent(JsonPropertyOrder.class)
    				||topClass.isAnnotationPresent(JsonAutoDetect.class);
    		if(annotationSeen&&topClass.isAnnotationPresent(JsonIncludeProperties.class)) {
    			includeProperties=new HashSet<>(Arrays.asList(topClass.getAnnotation(JsonIncludeProperties.class).value()));
    		}
    		if(annotationSeen&&topClass.isAnnotationPresent(JsonIgnoreProperties.class)) {
    			ignoreProperties=new HashSet<>(Arrays.asList(topClass.getAnnotation(JsonIgnoreProperties.class).value()));
    		}
    	}

		public int getField(String name) {
    		return name!=null?declaredFields.containsKey(name)?declaredFields.get(name):-1:-1;
    	}
    	
		public boolean isVisible(Field f) {
			if(f.getAnnotation(JsonProperty.class)==null) {
				if(f.getAnnotation(JacksonInject.class)!=null) {
					annotationSeen=true;
					return true;
				} else if(f.getAnnotation(JsonIgnore.class)!=null||f.getType().getAnnotation(JsonIgnoreType.class)!=null) {
					annotationSeen=true;
					return false;
				} else if(!Modifier.isStatic(f.getModifiers())) {
					String name=VisibilityType.Field.toName(f.getName());
					if(includeProperties!=null&&includeProperties.contains(name)) {
						return true;
					} else {
						JsonAutoDetect.Visibility visibility=VisibilityType.Field.getVisibility(f.getDeclaringClass());
						return visibility.isVisible(f)&&(ignoreProperties==null||!ignoreProperties.contains(name));
					}
				} else {
					return false;
				}
			} else {
				return true;
			}
		}
		
		public boolean isVisible(VisibilityType type,Method m) {
			if(m.getAnnotation(JsonProperty.class)==null) {
				if(m.getAnnotation(JsonIgnore.class)!=null||m.getReturnType().getAnnotation(JsonIgnoreType.class)!=null) {
					annotationSeen=true;
					return false;
				} else if(!Modifier.isStatic(m.getModifiers())) {
					String name=type.toName(m.getName());
					if(includeProperties!=null&&includeProperties.contains(name)) {
						return true;
					} else {
						JsonAutoDetect.Visibility visibility=VisibilityType.Field.getVisibility(m.getDeclaringClass());
						return visibility.isVisible(m)&&(ignoreProperties==null||!ignoreProperties.contains(name));
					}
				} else {
					return false;
				}
			} else {
				return true;
			}
		}
		
		public boolean isVisible(java.lang.reflect.Constructor<?> c) {
			return c.isAnnotationPresent(JsonCreator.class)&&VisibilityType.Creator.getVisibility(c.getDeclaringClass()).isVisible(c);
		}

		public Codec getCodec(Access access) {
			return codecs.get(access);
		}
		public void addCodec(Access access,Codec codec) {
			codecs.put(access,codec);
		}
		
		public Buffer createBuffer(String defaultValue) {
			return new Buffer(defaultValue);
		}
    }
}
