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

import com.fasterxml.jackson.annotation.JsonProperty;

import not.alexa.netobjects.types.ArrayTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition.Builder;
import not.alexa.netobjects.types.DefaultTypeLoader;
import not.alexa.netobjects.types.DefaultTypeLoader.TypeResolver;
import not.alexa.netobjects.types.JavaClass.Type;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.utils.TypeUtils;
import not.alexa.netobjects.utils.TypeUtils.ClassResolver;
import not.alexa.netobjects.utils.TypeUtils.ResolvedClass;

public class JacksonResolver implements TypeResolver {

    public JacksonResolver() {
    }

    @Override
    public TypeDefinition resolve(DefaultTypeLoader loader, ObjectType type) {
        if(type instanceof Type) try {
            Type t=(Type)type;
            Class<?> clazz=t.asLinkedLocal(loader.getClassLoader()).asClass();
            return defineFromClazz(loader,clazz);
        } catch(Throwable t) {
            t.printStackTrace();
        }
        return null;
    }

    private TypeDefinition defineFromClazz(DefaultTypeLoader loader,Class<?> clazz) {
        ClassResolver classResolver=TypeUtils.createClassResolver(clazz);
        ClassTypeDefinition def=new ClassTypeDefinition(clazz);
        Builder builder=def.createBuilder();
        return defineFromClazz0(loader,builder,classResolver,clazz).build();
    }
    
    private Builder defineFromClazz0(DefaultTypeLoader loader,Builder builder,ClassResolver resolver,Class<?> clazz) {
        if(Object.class.equals(clazz)) {
            return builder;
        }
        defineFromClazz0(loader,builder,resolver,clazz.getSuperclass());
        for(Field f:clazz.getDeclaredFields()) {
            JsonProperty prop=f.getAnnotation(JsonProperty.class);
            if(prop!=null) {
                String name=prop.value().length()>0?prop.value():f.getName();
                TypeDefinition type=resolveType(loader,resolver,resolver.resolve(f/*.getAnnotatedType()*/));
                if(type!=null) {
                    builder.createField(name, type).setOptional(true).setAbstract(true)./*setDefaultValue("test").*/build();
                }
            }
        }
        return builder;
    }

//    private TypeDefinition defineFromConstructor(DefaultTypeLoader loader,Class<?> clazz,Constructor<?> c) {
//        ClassResolver classResolver=TypeUtils.createClassResolver(clazz);
//        ClassTypeDefinition def=new ClassTypeDefinition(clazz);
//        Builder builder=def.createBuilder();
//        for(Parameter t:c.getParameters()) {
//            ResolvedClass p=classResolver.resolve(t.getParameterizedType());
//            String name=t.getName();
//            JsonProperty prop=t.getAnnotation(JsonProperty.class);
//            if(prop!=null&&prop.value().length()>0) {
//                name=prop.value();
//            }
//            TypeDefinition fieldType=resolveType(loader, classResolver,p);
//            builder.addField(name, fieldType);
//        }
//        return builder.build();
//    }
    
    private String getName(AnnotatedElement e,String defaultValue) {
        not.alexa.netobjects.api.Field f=e.getAnnotation(not.alexa.netobjects.api.Field.class);
        if(f!=null&&f.name().length()>0) {
            return f.name();
        }
        return defaultValue;
    }
    private TypeDefinition resolveType(DefaultTypeLoader loader,ClassResolver resolver,ResolvedClass clazz) {
        if(clazz.isArray()) {
            ResolvedClass[] parameters=clazz.getParameters();
            if(parameters.length==1) {
                TypeDefinition componentType=resolveType(loader,resolver,parameters[0]);
                return componentType==null?null:new ArrayTypeDefinition(resolveType(loader,resolver,parameters[0]));
            } else if(parameters.length==2) {
                String keyName=getName(parameters[0],"key");
                String valueName=getName(parameters[1],"value");
                TypeDefinition key=resolveType(loader,resolver,parameters[0]);
                TypeDefinition value=resolveType(loader,resolver,parameters[1]);
                if(key!=null&&value!=null) {
                    return new ArrayTypeDefinition(new ClassTypeDefinition().createBuilder()
                            .addField(keyName, key).addField(valueName, value).build()
                            );
                }
                
//                throw new RuntimeException();
                return null;
            } else {
                throw new RuntimeException();
            }
        } else if(clazz.hasParameters()) {
            ClassTypeDefinition def=new ClassTypeDefinition();
            return defineFromClazz0(loader, def.createBuilder(), resolver, clazz.getResolvedClass()).build();
//            throw new RuntimeException();
//            return null;
        } else {
            return loader.resolveType(clazz.getResolvedClass());
        }
    }
    
    

}
