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
package not.alexa.netobjects.utils;

import java.lang.reflect.AnnotatedArrayType;
import java.lang.reflect.AnnotatedParameterizedType;
import java.lang.reflect.AnnotatedType;
import java.lang.reflect.AnnotatedWildcardType;
import java.lang.reflect.Field;

import not.alexa.netobjects.utils.TypeUtils.AType;
import not.alexa.netobjects.utils.TypeUtils.ATypeFactory;

/**
 * Internal class only implementing the {@link ATypeFactory} using Java 8 features ({@code getAnnotatedSuperclass},...).
 * These are not available on Android, where instantiation of this factory fails.
 * 
 * @author notalexa
 *
 */
class VM8TypeFactory implements ATypeFactory {
	private AType[] decorate(AnnotatedType[] args) {
		AType[] result=new AType[args.length];
		for(int i=0;i<result.length;i++) {
			result[i]=new AType(args[i].getType(),args[i]);
		}
		return result;
	}
	
	VM8TypeFactory() {
		getClass().getAnnotatedSuperclass();
	}

	@Override
	public AType[] getAnnotatedActualTypeArguments(AType annotatedType) {
		return decorate(((AnnotatedParameterizedType)annotatedType.annotationHolder).getAnnotatedActualTypeArguments());
	}

	@Override
	public AType getAnnotatedType(Field f) {
		return new AType(f.getGenericType(),f.getAnnotatedType(),f);
	}

	@Override
	public AType getAnnotatedSuperclass(Class<?> c) {
		return new AType(c.getGenericSuperclass(),c.getAnnotatedSuperclass());
	}

	@Override
	public AType[] getAnnotatedInterfaces(Class<?> c) {
		return decorate(c.getAnnotatedInterfaces());
	}

	@Override
	public AType[] getAnnotatedUpperBounds(AType type) {
		return decorate(((AnnotatedWildcardType)type.annotationHolder).getAnnotatedUpperBounds());
	}

	@Override
	public AType getAnnotatedGenericComponentType(AType annotatedType) {
		AnnotatedType type=((AnnotatedArrayType)annotatedType.annotationHolder).getAnnotatedGenericComponentType();
		return new AType(type.getType(),type);
	}

}
