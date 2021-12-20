package not.alexa.coding;

import java.util.HashMap;
import java.util.Map;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.api.NetworkObject;
import not.alexa.netobjects.coding.Decoder.Buffer;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.ArrayTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.access.AbstractClassAccess;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.AccessContext;
import not.alexa.netobjects.types.access.AccessFactory;
import not.alexa.netobjects.types.access.DefaultAccessibleObject;
import not.alexa.netobjects.types.access.Constructor;
import not.alexa.netobjects.utils.ArrayUtils;
import not.alexa.netobjects.types.EnumTypeDefinition;
import not.alexa.netobjects.types.Namespace;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.PrimitiveTypeDefinition;
import not.alexa.netobjects.types.TypeDefinition;

public class Data {
	private static ClassTypeDefinition DESCR=new ClassTypeDefinition(Data.class);
	static {
		DESCR.createBuilder()
			.setEnableObjectRefs(true)
			.createField("text", PrimitiveTypeDefinition.getTypeDescription(String.class)).setOptional(true).build()
			.createField("@index", PrimitiveTypeDefinition.getTypeDescription(Integer.class)).setDefaultValue(0).build()
			.createField("@state", new EnumTypeDefinition(State.class)).setOptional(true).build()
			.createField("ref", PrimitiveTypeDefinition.getTypeDescription(Object.class)).setOptional(true).build()
			.createField("data", DESCR).setOptional(true).build()
			.createField("list", new ArrayTypeDefinition(PrimitiveTypeDefinition.getTypeDescription(String.class))).setOptional(true).build()
			.createField("matrix", new ArrayTypeDefinition(new ArrayTypeDefinition(PrimitiveTypeDefinition.getTypeDescription(String.class)))).setOptional(true).build()
			.createField("map",new ArrayTypeDefinition(new ClassTypeDefinition().createBuilder()
				.addField("@k", PrimitiveTypeDefinition.getTypeDescription(String.class))
				.addField("v", PrimitiveTypeDefinition.getTypeDescription(Integer.class))
				.build())).setOptional(true).build()
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
	
	public static class ClassAccess extends AbstractClassAccess implements Access {
		public ClassAccess(AccessFactory factory,Constructor constructor) {
			super(factory,DESCR,constructor);
		}

		@Override
		public Object getField(Object o, int index) throws BaseException {
			Data d=(Data)o;
			switch(index) {
				case 0:return d.text;
				case 1:return d.index;
				case 2:return d.state;
				case 3:return d.ref;
				case 4:return d.data;
				case 5:return ArrayUtils.nullIfEmpty(d.list);
				case 6:return ArrayUtils.nullIfEmpty(d.matrix);
				case 7:return ArrayUtils.nullIfEmpty(d.map);
			}
			return null;
		}


		@Override
		public Access createFieldAccess(Field f) throws BaseException {
			switch(f.getIndex()) {
				case 5:return forArray(f.getType(),String[].class);
				case 6:return forArray(f.getType(),String[][].class);
				case 7:return forMap(f.getType(),Map.class);
				default: return super.createFieldAccess(f);
			}
		}

		@Override
		public void setField(Object o,int index, Object v) throws BaseException {
			Data d=(Data)o;
			switch(index) {
				case 0:d.text=(String)v;
					break;
				case 1:d.index=(Integer)v;
					break;
				case 2:d.state=(State)v;
					break;
                case 3:d.ref=v;
                    break;
				case 4:d.data=(Data)v;
					break;
				case 5:d.list=(String[])v;
					break;
				case 6:d.matrix=(String[][])v;
					break;
				case 7:d.map=(Map<String,Integer>)v;
					break;
			}
		}
//
//		@Override
//		public AccessibleObject newInstance(AccessContext context) {
//			return new DefaultAccessibleObject(this,new Data());
//		}
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

	enum State {
		active,passive,failed;
	}
}
