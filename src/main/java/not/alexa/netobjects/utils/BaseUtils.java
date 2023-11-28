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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.coding.Decoder;
import not.alexa.netobjects.coding.yaml.YamlCodingScheme;

/**
 * Basic utility class
 * 
 * @author notalexa
 *
 */
public class BaseUtils {

	private BaseUtils() {
	}
	
	/**
	 * Resolve an input stream. The method resolves paths starting with {@code cp://} using 
	 * the context classloader and a path containing {@code ://} as an URL. Otherwise, the path
	 * is interpreted as a normal file path.
	 *  
	 * @param context the context to use for resolving the path
	 * @param path the path
	 * @return the corresponding input stream
	 * @throws IOException if an error (especially if not found) occurs
	 */
	public static InputStream resolve(ClassLoader loader,String path) throws IOException {
		InputStream result=null;
		if(path.startsWith("cp://")) {
			result=loader.getResourceAsStream(path.substring("cp://".length()));
		} else if(path.indexOf("://")>0) {
			result=new URL(path).openStream();
		} else {
			result=new FileInputStream(path);
		}
		if(result==null) {
			throw new FileNotFoundException(path);
		}
		return result;
	}
	
	/**
	 * Resolves the startup class of type {@code T}.
	 * 
	 * @param <T> the startup type
	 * @param context the context to uses for resolving
	 * @param clazz the startup class
	 * @param args arguments. The first argument is expected to denote a path to the yaml file containing
	 * the startup instance 
	 * @return the startup instance
	 * @throws BaseException if an error occurs
	 */
	public static <T> T resolveStartupInstance(Context context,Class<T> clazz,String[] args) throws BaseException {
		if(args.length>0) {
			T server=null;
			try(InputStream stream=resolve(context.getTypeLoader().getClassLoader(),args[0]);
				Decoder decoder=YamlCodingScheme.CONFIGURATION_SCHEME.newBuilder().setRootType(context.resolveType(clazz)).build().createDecoder(context, stream)) {
				server=decoder.decode(clazz);
			} catch(IOException e) {
				return BaseException.throwException(e);
			}
			if(server==null) {
				throw new BaseException(BaseException.NOT_FOUND,"object of type "+clazz.getName()+" in "+args[0]);
			}
			return server;
		} else {
			throw new BaseException(BaseException.BAD_REQUEST,"Expected the filename as first argument");
		}
	}
}
