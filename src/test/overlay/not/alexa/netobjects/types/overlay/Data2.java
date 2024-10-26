package not.alexa.netobjects.types.overlay;

import java.util.HashMap;
import java.util.Map;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.api.NetworkObject;
import not.alexa.netobjects.types.ArrayTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.EnumTypeDefinition;
import not.alexa.netobjects.types.PrimitiveTypeDefinition;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.access.AbstractClassAccess;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.AccessContext;
import not.alexa.netobjects.types.access.AccessFactory;
import not.alexa.netobjects.types.access.RuntimeInfo;
import not.alexa.netobjects.utils.ArrayUtils;

public class Data2 {
	private static ClassTypeDefinition DESCR=new ClassTypeDefinition(Data2.class);
	static {
		DESCR.createBuilder()
			.setEnableObjectRefs(true)
			.addField("text", PrimitiveTypeDefinition.getTypeDescription(String.class))
			.addField("@index", PrimitiveTypeDefinition.getTypeDescription(Integer.class))
			.addField("@state", new EnumTypeDefinition(State.class))
			.addField("ref", PrimitiveTypeDefinition.getTypeDescription(Object.class))
			.addField("data", DESCR)
			.addField("list", new ArrayTypeDefinition(PrimitiveTypeDefinition.getTypeDescription(String.class)))
			.addField("matrix", new ArrayTypeDefinition(new ArrayTypeDefinition(PrimitiveTypeDefinition.getTypeDescription(String.class))))
			.addField("map",new ArrayTypeDefinition(new ClassTypeDefinition().createBuilder()
					.addField("@k", PrimitiveTypeDefinition.getTypeDescription(String.class))
					.addField("v", PrimitiveTypeDefinition.getTypeDescription(Integer.class))
					.build()))
				.build();
	}
	public static TypeDefinition getTypeDescription() {
		return DESCR;
	}
	protected String text;
	protected int index;
	protected State state;
	protected Data2 data;
	protected Object ref;
	protected String[] list;
	protected String[][] matrix;
	protected Map<String,Integer> map;
	
	public Data2() {
	}

	public Data2(String text,int index,String...list) {
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
	
	public static class ClassAccess extends AbstractClassAccess implements Access {
		public ClassAccess(AccessFactory factory,RuntimeInfo constructor) {
			super(factory,DESCR,constructor);
		}

		@Override
		public Object getField(AccessContext context,Object o, int index) throws BaseException {
			Data2 d=(Data2)o;
			switch(index) {
				case 0:return d.text;
				case 1:return d.index;
				case 2:return d.state;
				case 3:return d.data;
				case 4:return d.ref;
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
		public void setField(AccessContext context,Object o,int index, Object v) throws BaseException {
			Data2 d=(Data2)o;
			switch(index) {
				case 0:d.text=(String)v;
					break;
				case 1:d.index=(Integer)v;
					break;
				case 2:d.state=(State)v;
					break;
				case 3:d.data=(Data2)v;
					break;
				case 4:d.ref=v;
					break;
				case 5:d.list=(String[])v;
					break;
				case 6:d.matrix=(String[][])v;
					break;
				case 7:d.map=(Map<String,Integer>)v;
					break;
			}
		}
	}

   public String helloWorld(Context context) throws Throwable {
        return "Hello World (2)";
    }
	    
    @NetworkObject(id="hello-universe")
    public String helloUniverse(Context context) throws Throwable {
        return "Hello Universe (2)";
    }

	enum State {
		active,passive,failed;
	}
}
