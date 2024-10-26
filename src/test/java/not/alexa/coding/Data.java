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
package not.alexa.coding;

import java.util.HashMap;
import java.util.Map;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.api.NetworkObject;
import not.alexa.netobjects.types.ArrayTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.EnumTypeDefinition;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.PrimitiveTypeDefinition;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.access.AccessContext;

public class Data {
	private static ClassTypeDefinition DESCR=new ClassTypeDefinition(Data.class);
	static {
		DESCR.createBuilder()
			.setEnableObjectRefs(true)
			.createField("text", PrimitiveTypeDefinition.getTypeDescription(String.class)).setOptional(true).build()
			.createField("index", PrimitiveTypeDefinition.getTypeDescription(Integer.class))
			    .addTag("XML","@index").setDefaultValue(0).build()
			.createField("state", new EnumTypeDefinition(State.class))
			    .addTag("XML","@state").setOptional(true).build()
			.createField("ref", PrimitiveTypeDefinition.getTypeDescription(Object.class)).setOptional(true).build()
			.createField("data", DESCR).setOptional(true).build()
			.createField("list", new ArrayTypeDefinition(PrimitiveTypeDefinition.getTypeDescription(String.class))).setOptional(true).build()
			.createField("matrix", new ArrayTypeDefinition(new ArrayTypeDefinition(PrimitiveTypeDefinition.getTypeDescription(String.class)))).setOptional(true).build()
			.createField("map",new ArrayTypeDefinition(new ClassTypeDefinition().createBuilder()
				.createField("k", PrimitiveTypeDefinition.getTypeDescription(String.class))
				    .addTag("XML","@k").setOptional(true).build()
				.addField("v", PrimitiveTypeDefinition.getTypeDescription(Integer.class))
				.build())).setOptional(true).addHint("test").build()
			.createMethod("helloWorld")
			    .setReturnTypes(PrimitiveTypeDefinition.getTypeDescription(String.class))
			    .build()
	        .createMethod("helloUniverse")
	            .setTypes(ObjectType.resolve("jvm:"+Data.class.getName()+"::hello-universe"))
                .setParameterTypes(PrimitiveTypeDefinition.getTypeDescription(String.class))
                .setReturnTypes(PrimitiveTypeDefinition.getTypeDescription(String.class))
                .build()
			.build();
	}
	public static TypeDefinition getTypeDescription() {
		return DESCR;
	}
	protected String text;
	protected int index;
	protected State state;
	protected Data data;
	protected Object ref;
	protected String[] list;
	protected String[][] matrix;
	protected Map<String,Integer> map;
	
	public Data() {
	}

	public Data(String text,int index,String...list) {
		this.text=text;
		this.index=index;
		state=State.active;
		data=this;
		ref=this;
		this.list=list;
		this.matrix=new String[][] { list,list};
		this.map=new HashMap<String, Integer>();
		for(int i=0;i<list.length;i++) {
			map.put(list[i],i);
		}
	}
	
	@NetworkObject
	@NetworkObject(ns="test",id="test")
	public String test(Context context,String msg) throws BaseException {
	    return msg;
	}

	public void list(AccessContext context,String[] list) {
		this.list=list;
	}

	public void map(Context context,Map<String,Integer> map) {
		this.map=map;
	}
	
	public Data setRef(Object ref) {
	    this.ref=ref;
	    return this;
	}
	
	public String helloWorld(Context context) throws Throwable {
	    return "Hello World";
	}
	
	@NetworkObject(id="hello-universe")
    public String helloUniverse(Context context) throws Throwable {
        return "Hello Universe";
    }
	
	protected Object finish(AccessContext context) {
		return this;
	}

	public enum State {
		active,passive,failed;
	}
}
