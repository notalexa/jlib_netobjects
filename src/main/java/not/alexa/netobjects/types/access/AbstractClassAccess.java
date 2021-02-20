package not.alexa.netobjects.types.access;

import java.util.Map;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.types.ArrayTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.TypeDefinition;

public abstract class AbstractClassAccess implements Access {
	protected ClassTypeDefinition classType;
	protected Access[] fieldAccess;
	protected AccessFactory factory;
	public AbstractClassAccess(AccessFactory factory,ClassTypeDefinition classType) {
		this.classType=classType;
		this.factory=factory;
		fieldAccess=new Access[classType.getFields().length];
	}

	@Override
	public ClassTypeDefinition getType() {
		return classType;
	}

	@Override
	public Field[] getFields() {
		return classType.getFields();
	}

	@Override
	public Object getField(Object o, Field f) throws BaseException {
		return getField(o,f.getIndex());
	}
	
	protected abstract Object getField(Object o,int index) throws BaseException;

	@Override
	public void setField(Object o, Field f, Object v) throws BaseException {
		setField(o,f.getIndex(),v);
	}
	
	protected abstract void setField(Object o,int index,Object v) throws BaseException;

	@Override
	public Access getFieldAccess(Field f) throws BaseException {
		Access access=fieldAccess[f.getIndex()];
		if(access==null) {
			synchronized (this) {
				access=fieldAccess[f.getIndex()];
				if(access==null) {
					access=fieldAccess[f.getIndex()]=createFieldAccess(f);
				}
			}
		}
		return access;
	}
	
	protected Access createFieldAccess(Field f) throws BaseException {
		return factory.resolve(this,f.getType());
	}
	
	protected Access forArray(TypeDefinition description,Class<?> clazz) {
		if(clazz.isArray()) {
			return new ArrayTypeAccess(description, forArray(((ArrayTypeDefinition)description).getComponentType(),clazz.getComponentType()),clazz);
		} else {
			return factory.resolve(this,description);
		}
	}
	
	protected Access forMap(TypeDefinition description,Class<? extends Map> clazz) {
		return new ArrayTypeAccess(description,new MapEntryAccess(factory,this,(ClassTypeDefinition)((ArrayTypeDefinition)description).getComponentType()), clazz);
	}
}
