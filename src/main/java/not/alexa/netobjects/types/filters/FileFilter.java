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
package not.alexa.netobjects.types.filters;

import java.io.File;

import not.alexa.netobjects.types.access.AccessContext;
import not.alexa.netobjects.types.access.CodingFilter;
import not.alexa.netobjects.types.access.FieldAccessor.Getter;
import not.alexa.netobjects.types.access.FieldAccessor.Setter;

/**
 * Filter mediating between {@linkplain File} and {@linkplain String}.
 * 
 * @author notalexa
 */
public class FileFilter implements CodingFilter<File,String> {

	@Override
	public Class<File> getSourceClass() {
		return File.class;
	}
	
	public Class<String> getCodingClass() {
		return String.class;
	}

	@Override
	public Getter filter(Getter getter) {
		return new Getter() {
			@Override
			public Object invoke(AccessContext context, Object o) throws Throwable {
				Object v=getter.invoke(context, o);
				return v==null?null:((File)v).getPath();
			}
		};
	}

	@Override
	public Setter filter(Setter setter) {
		return new Setter() {
			@Override
			public void invoke(AccessContext context, Object o, Object v) throws Throwable {
				setter.invoke(context, o, v==null?null:new File((String)v));
			}
		};
	}
}

