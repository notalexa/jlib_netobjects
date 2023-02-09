/*
 * Copyright (C) 2021 Not Alexa
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
package not.alexa.netobjects.types.access;

import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import not.alexa.netobjects.Adaptable;
import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.api.Overlay;
import not.alexa.netobjects.types.AccessibleObject;
import not.alexa.netobjects.types.ArrayTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.JavaClass;
import not.alexa.netobjects.types.JavaClass.Type;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.TypeLoader;
import not.alexa.netobjects.types.TypeLoader.LinkedLocal;
import not.alexa.netobjects.types.access.Access.SimpleTypeAccess;
import not.alexa.netobjects.utils.TypeUtils;
import not.alexa.netobjects.utils.WeakKeyMap;
import not.alexa.netobjects.utils.WeakValueMap;

/**
 * Default implementation of {@link AccessFactory}. This access is suitable for type definitions which have a local java class representation. 
 * The implementation resolves access as follows:
 * <ul>
 * <li>If the definition is primitive, an enumeration or an interface, {@link SimpleTypeAccess} is used.
 * <li>If the definition is an array, no access is returned.
 * <li>If the definition is a class type access is resolved as follows:
 * <ol>
 * <li>If the type has a java class representation, the access class is constructed using the name of the java class with <code>$ClassAccess</code>
 * appended. This class is resolved using the class loader of the referrer or context. The class should be loadable, implement {@link Access} and should contain a constructor with argument {@link AccessFactory}
 * which is called with argument this access factory.
 * <li>If additional resolvers are present, each of this resolver is called in the registered order. The first one resolving access is taken.
 * <li>Finally, an {@link AnonymousClassAccess} is created (which creates an error whenever a new instance is requested and resolves fields using
 * this factory) if the access is unknown.
 * </ol>
 * </ul>
 * 
 * 
 * As for the default type loader implementation, 
 * the default access factpry can be extended using {@link AccessResolver}. Such resolvers can be registered globally for future instances of a type loader using {@link AccessResolver#addAccessResolver(AccessFactory)}
 * or using the constructor {@link DefaultAccessFactory#DefaultTypeLoader(List)}.
 * 
 * @author notalexa
 * @see AccessResolver
 *
 */
public class DefaultAccessFactory extends Adaptable.Default implements  AccessFactory {
    private static DefaultAccessFactory INSTANCE;
    private static List<AccessResolver> defaultResolvers;
    
    /**
     * Add a resolver to the list of default resolvers. Resolvers already contained in the list (with respect to {@link Object#equals(Object)}) are ignored.
     * <p><b>Registration of a new resolver doesn't have any effect on factories already created.</b>
     * @param resolver the resolver to add.
     * @see AccessResolver
     */
    public static synchronized void addAccessResolver(AccessResolver resolver) {
        if(defaultResolvers==null) {
            defaultResolvers=new ArrayList<AccessResolver>();
        }
        if(!defaultResolvers.contains(resolver)) {
            defaultResolvers.add(resolver);
        }
        INSTANCE=null;
    }
    
    private final Caster NULL_CASTER=new Caster() {
        @Override
        public boolean needsCast() {
            return false;
        }
    };
    
    private Caster INTERFACE_CASTER=new Caster() {
        @Override
        public <T> T upcast(Context context,T o) throws BaseException {
            Caster caster=resolveCaster(context.getTypeLoader(),o.getClass());
            return caster.upcast(context,o);
        }

        @Override
        public CasterContext upcast(CasterContext context, Object o) throws BaseException {
            if(o!=null) {
                Caster caster=resolveCaster(context.getContext().getTypeLoader(),o.getClass());
                return caster.upcast(context,o);
            } else {
                return context.getChildContext(null,null);
            }
        }
    };
        
    protected AccessResolver[] resolvers;
    private WeakValueMap<Class<?>,RootAccessHolder> loaded=new WeakValueMap<>();
    private WeakKeyMap<TypeLoader,Map<Object,AccessHolder>> loadedTypeMaps=new WeakKeyMap<>();

	public DefaultAccessFactory() {
	    this(defaultResolvers);
	}
	
	public DefaultAccessFactory(List<AccessResolver> resolvers) {
	    this.resolvers=resolvers==null||resolvers.size()==0?null:resolvers.toArray(new AccessResolver[resolvers.size()]);
	}
	
	static AccessFactory getDefault() {
	    AccessFactory current=INSTANCE;
	    if(current==null) {
	        synchronized(DefaultAccessFactory.class) {
	            current=INSTANCE;
	            if(current==null) {
	                current=INSTANCE=new DefaultAccessFactory();
	            }
	        }
	    }
	    return current;
	}

	@Override
    public final Access resolve(Context context,TypeDefinition type) {
	    RootAccessHolder rootAccess=resolve(context.getTypeLoader().getClassLoader(),type);
	    return rootAccess==null?null:rootAccess.getAccess();
	}

	protected synchronized AccessHolder resolveHolder(TypeLoader typeLoader,Type javaType) {
	    if(javaType!=null) {
            Map<Object,AccessHolder> typeMap=getTypeMap(typeLoader);
            AccessHolder accessHolder=typeMap.get(javaType);
            if(accessHolder==null) {
                accessHolder=new AccessHolder(typeLoader, getClassAccess(typeLoader.getLinkedClass(javaType),javaType));
                typeMap.put(javaType,accessHolder);
                typeMap.put(accessHolder.getAccessClass(),accessHolder);
            }
            return accessHolder;
	    }
	    return null;
	}

    @Override
    public Constructor resolve(Context context, Type javaType) {
        AccessHolder accessHolder=javaType==null?null:resolveHolder(context.getTypeLoader(), javaType);
        return accessHolder==null?null:accessHolder.getConstructor();
    }
	
	protected Map<Object,AccessHolder> getTypeMap(TypeLoader loader) {
        Map<Object,AccessHolder> typeMap=loadedTypeMaps.get(loader);
        if(typeMap==null) {
            typeMap=new HashMap<Object, AccessHolder>();
            loadedTypeMaps.put(loader,typeMap);
        }
        return typeMap;
	}
	
	@Override
	public final Access resolve(Access referrer,TypeDefinition type) {
	    RootAccessHolder accessHolder=resolve(referrer.getAccessLoader(),type);
	    return accessHolder==null?null:accessHolder.getAccess();
	}
	
	public <T> T upcast(Context context,T t) {
	    if(t==null) {
	        return null;
	    }
	    Caster caster=resolveCaster(context.getTypeLoader(),t.getClass());
	    try {
	        return caster.upcast(context,t);
	    } catch(Throwable e) {
	        return t;
	    }
	}
	
	protected Caster resolveCaster(TypeLoader loader,Class<?> clazz) {
	    Map<Object,AccessHolder> typeMap=getTypeMap(loader);
	    AccessHolder accessHolder=typeMap.get(clazz);
	    if(accessHolder==null) {
	        TypeDefinition typeDef=loader.resolveType(clazz);
	        if(typeDef!=null) {
	            accessHolder=resolveHolder(loader, ObjectType.createClassType(clazz));
	        }
	    }
	    if(accessHolder!=null) {
	        return accessHolder.getCaster();
	    }
	    return NULL_CASTER;
	}
	
	protected synchronized RootAccessHolder getClassAccess(Class<?> clazz,Type type) {
	    RootAccessHolder access=loaded.get(clazz);
	    if(access==null) {
	        //System.out.println("CREATE FOR "+clazz+", (type="+type+") in "+this);
	        access=new RootAccessHolder(clazz, type==null?ObjectType.createClassType(clazz):type);
	        loaded.put(clazz, access);
	    }
	    return access;
	}
	
	/**
	 * Useful to be called in {@link AccessResolver#resolve(DefaultAccessFactory, ClassLoader, TypeDefinition)} this method resolves access as described in the
	 * type description above.
	 * 
	 * @param usedClassLoader the class loader to use
	 * @param type the type to resolve
	 * @return the access holder for the type or <code>null</code> if no access is provided.
	 */
    protected RootAccessHolder resolve(ClassLoader usedClassLoader,TypeDefinition type) {
	    Type classType=type.getJavaClassType();
	    Class<?> clazz=classType==null?null:classType.asLinkedLocal(usedClassLoader).asClass();
	    RootAccessHolder accessHolder=clazz==null?null:getClassAccess(clazz,classType);
		if(accessHolder!=null&&!accessHolder.hasAccessCreated()) {
		    accessHolder.createAccess(type);
		}
		return accessHolder;
	}
	
	/**
	 * For testing only
	 */
	protected int gc() {
	    int n=loadedTypeMaps.size();
	    loaded.size();
	    return n;
	}

    @Override
    public AccessFactory forContext(Context context) {
        return new AccessFactory() {
            Map<Object,AccessHolder> typeMap;

            @Override
            public Access resolve(Context context, TypeDefinition type) {
                if(typeMap==null) {
                    typeMap=getTypeMap(context.getTypeLoader());
                }
                Type classType=type.getJavaClassType();
                AccessHolder accessHolder=classType==null?null:typeMap.get(classType);
                return accessHolder==null||!accessHolder.hasAccessCreated()?DefaultAccessFactory.this.resolve(context, type):accessHolder.getAccess();
            }

            @Override
            public Access resolve(Access referrer, TypeDefinition type) {
                return DefaultAccessFactory.this.resolve(referrer, type);
            }

            @Override
            public Constructor resolve(Context context, Type classType) {
                if(typeMap==null) {
                    typeMap=getTypeMap(context.getTypeLoader());
                }
                AccessHolder accessHolder=classType==null?null:typeMap.get(classType);
                return accessHolder==null?DefaultAccessFactory.this.resolve(context, classType):accessHolder.getConstructor();
            }

            @Override
            public <T> T upcast(Context context, T t) {
                return DefaultAccessFactory.this.upcast(context, t);
            }

            @Override
            public AccessFactory forContext(Context context) {
                return DefaultAccessFactory.this.forContext(context);
            }
        };
    }

    private class RootAccessHolder {
        Class<?> clazz;
        boolean accessCreated;
        Access access;
        Constructor constructor;
        
        private RootAccessHolder(Class<?> clazz,Type type) {
            this(clazz,new Constructor.DefaultConstructor(type, clazz));
        }
        
        private RootAccessHolder(Class<?> clazz,Constructor constructor) {
            this.clazz=clazz;
            this.constructor=constructor;
        }
        
        public boolean hasAccessCreated() {
            return accessCreated;
        }
        public Access getAccess() {
            return access;
        }
        
        public RootAccessHolder updateAccess(Access access) {
            this.access=access;
            this.accessCreated=true;
            return this;
        }
        
        public Constructor getConstructor() {
            return constructor;
        }
        
        void createAccess(TypeDefinition type) {
            if(!hasAccessCreated()) {
                Access access=null;
                switch(type.getFlavour()) {
                    case InterfaceType:
                    case EnumType:
                    case PrimitiveType:access=new SimpleTypeAccess(DefaultAccessFactory.this,type);
                        break;
                    case ArrayType:
                        break;
                    case ClassType:
                        JavaClass.Type javaType=type.getJavaClassType();
                        if(javaType!=null) try {
                            Class<?> accessClass=Class.forName(javaType.getName()+"$ClassAccess",true,clazz.getClassLoader());
                            try {
                                java.lang.reflect.Constructor<?> c=accessClass.getConstructor(AccessFactory.class,Constructor.class);
                                if(Access.class.isAssignableFrom(accessClass)) {
                                    access=(Access)c.newInstance(DefaultAccessFactory.this,TypeUtils.isFinal(clazz)?getConstructor():new Constructor.OverlayConstructor(javaType,getConstructor()));
                                }
                            } catch(Throwable t) {}
                            try {
                                java.lang.reflect.Constructor<?> c=accessClass.getConstructor(AccessFactory.class);
                                if(Access.class.isAssignableFrom(accessClass)) {
                                    access=(Access)c.newInstance(DefaultAccessFactory.this);
                                }
                            } catch(Throwable t) {}
                        } catch(Throwable t) {
                        }
                        if(access==null&&resolvers!=null) for(AccessResolver resolver:DefaultAccessFactory.this.resolvers) {
                            if((access=resolver.resolve(DefaultAccessFactory.this, clazz.getClassLoader(), type))!=null) {
                                break;
                            }
                        }
                        if(access==null) {
                            access=new ReflectionClassAccess(DefaultAccessFactory.this, clazz, (ClassTypeDefinition)type, TypeUtils.isFinal(clazz)?getConstructor():new Constructor.OverlayConstructor(javaType,getConstructor()));
                        }
                        break;
                }
                updateAccess(access);
            }
        }
    }

    private class AccessHolder {
        WeakReference<TypeLoader> loader;
        RootAccessHolder root;
        Caster caster;
        
        private AccessHolder(TypeLoader loader,RootAccessHolder root) {
            this.loader=new WeakReference<>(loader);
            this.root=root;
            if(!root.hasAccessCreated()) {
                TypeDefinition def=loader.resolveType(root.clazz);
                if(def!=null) {
                    root.createAccess(def);
                }
            }
        }
        
        public boolean hasAccessCreated() {
            return root.accessCreated;
        }
        public Access getAccess() {
            return root.access;
        }
        
        Class<?> getAccessClass() {
            return root.clazz;
        }
        
        public Constructor getConstructor() {
            return root.constructor;
        }
        
        public Caster getCaster() {
            if(caster==null) {
                TypeLoader l=loader.get();
                if(l!=null) {
                    caster=resolveCaster(l, l.resolveType(getAccessClass()));    
                } else {
                    caster=NULL_CASTER;
                }
            }
            return caster;
        }
    }
	
	public interface AccessResolver {
	    /**
	     * Resolve access . The implementation should
	     * take care of infinite loops while looking up access using the supplied factpry.
	     * 
	     * @param factory the default access factory requesting the access
	     * @param type the type we need access for
	     * @return the resolved type definition or <code>null/code> if not resolveable.
	     */
	    public Access resolve(DefaultAccessFactory factory,ClassLoader currentClassLoader,TypeDefinition type);
	}
	
    private class Caster {
        boolean needsCast() {
            return true;
        }
        public <T> T upcast(Context context,T o) throws BaseException {
            return o;
        }
        public CasterContext upcast(CasterContext context,Object o) throws BaseException {
            return context.getChildContext(o,o);
        }
    }
    
    private class ArrayCaster extends Caster {
        private Caster componentCaster;
        private ArrayCaster(Caster componentCaster) {
            this.componentCaster=componentCaster;
        }

        @Override
        public CasterContext upcast(CasterContext context,Object o) throws BaseException {
            if(o==null) {
                return context.getChildContext(null,null);
            }
            Class<?> clazz=o.getClass();
            if(clazz.isArray()) {
                int n=Array.getLength(o);
                if(n>0) {
                    Object a=Array.newInstance(clazz.getComponentType(),n);
                    CasterContext arrayContext=context.getChildContext(o,a);
                    for(int i=0;i<n;i++) {
                        Object s=Array.get(o,i);
                        CasterContext child=componentCaster.upcast(arrayContext,s);
                        arrayContext.update(child);
                        Array.set(a, i, child.n);
                    }
                    return arrayContext;
                }
            } else if(Collection.class.isAssignableFrom(o.getClass())) try {
                Collection<?> c=(Collection<?>)o;
                if(c.size()>0) {
                    Collection<Object> a=(Collection)o.getClass().newInstance();
                    CasterContext arrayContext=context.getChildContext(o,a);
                    for(Object s:c) {
                        CasterContext child=componentCaster.upcast(context,s);
                        arrayContext.update(child);
                        a.add(child.n);
                    }
                    return arrayContext;
                }
            } catch(IllegalAccessException|InstantiationException e) {
            } else if(Map.class.isAssignableFrom(o.getClass())) try {
                Map<Object,Object> c=(Map<Object,Object>)o;
                if(c.size()>0) {
                    Map<Object,Object> a=(Map<Object,Object>)o.getClass().newInstance();
                    CasterContext arrayContext=context.getChildContext(o,a);
                    for(Map.Entry<Object,Object> s:c.entrySet()) {
                        CasterContext child=componentCaster.upcast(context,s);
                        arrayContext.update(child);
                        Map.Entry<Object,Object> t=(Map.Entry<Object,Object>)child.n;
                        a.put(t.getKey(),t.getValue());
                    }
                    return arrayContext;
                }
            } catch(IllegalAccessException|InstantiationException e) {
            }
            return context.getChildContext(o,o);
        }
    }

    private Caster resolveCaster(TypeLoader loader,TypeDefinition typeDef) {
        Map<Type,Boolean> overlayMap=new HashMap<>();
        Type type=typeDef.getJavaClassType();
        boolean b=false;
        overlayMap.put(type,b);
        switch(typeDef.getFlavour()) {
            case PrimitiveType:
            case EnumType:
            case UnknownType:
            case MethodType:return NULL_CASTER;
            case ArrayType:Caster componentCaster=resolveCaster(loader,((ArrayTypeDefinition)typeDef).getComponentType());
                return componentCaster.needsCast()?new ArrayCaster(componentCaster):NULL_CASTER;
            case InterfaceType:b=loader.hasOverlays(type.asLinkedLocal(loader.getClassLoader()).asClass());
                return b?INTERFACE_CASTER:NULL_CASTER;
            case ClassType:AccessHolder accessHolder=resolveHolder(loader, type);
                if(accessHolder!=null&&accessHolder.getConstructor().isOverlay(loader)) {
                    return new ClassCaster((ClassTypeDefinition)typeDef,accessHolder);
                } else for(Field f:((ClassTypeDefinition)typeDef).getFields()) {
                    if(needsCaster(loader,f.getType(),overlayMap)) {
                        return new ClassCaster((ClassTypeDefinition)typeDef,accessHolder);
                    }
                }
                break;
        }
        return NULL_CASTER;
    }

    private boolean needsCaster(TypeLoader loader,TypeDefinition typeDef,Map<Type,Boolean> overlayMap) {
        Type type=typeDef.getJavaClassType();
        Boolean b=overlayMap.get(type);
        if(b==null) {
            overlayMap.put(type,b=false);
            switch(typeDef.getFlavour()) {
                case PrimitiveType:
                case EnumType:
                case UnknownType:
                case MethodType:return false;
                case ArrayType:b=needsCaster(loader,((ArrayTypeDefinition)typeDef).getComponentType(),overlayMap);
                    overlayMap.put(type,b);
                    return b;
                case InterfaceType:b=loader.hasOverlays(type.asLinkedLocal(loader.getClassLoader()).asClass());
                    overlayMap.put(type,b);
                    return b;
                case ClassType:LinkedLocal linkedLocal=loader.getLinkedLocal(type);
                    if(linkedLocal.asClass().getAnnotation(Overlay.class)!=null) {
                        b=true;
                        overlayMap.put(type,b);
                        return b;
                    } else for(Field f:((ClassTypeDefinition)typeDef).getFields()) {
                        if(needsCaster(loader,f.getType(), overlayMap)) {
                            b=true;
                            overlayMap.put(type,b);
                            return b;
                        }
                    }
                    break;
            }
        }
        return b;
    }
    
    private class ClassCaster extends Caster {
        ClassTypeDefinition classDef;
        Constructor c;
        Access access;
        Caster[] fieldCaster;
        public ClassCaster(ClassTypeDefinition classDef,AccessHolder access) {
            this.classDef=classDef;
            this.access=access.getAccess();
            this.c=access.getConstructor();
        }
        
        @Override
        public <T> T upcast(Context context, T o) throws BaseException {
            return (T)upcast(new CasterContext(context),o).n;
        }
        
        private Caster[] resolveFieldCasters(CasterContext context) {
            if(fieldCaster==null) {
                Field[] fields=classDef.getFields();
                fieldCaster=new Caster[fields.length];
                for(int i=0;i<fields.length;i++) {
                    fieldCaster[i]=resolveCaster(context.getContext().getTypeLoader(), fields[i].getType());
                }
            }
            return fieldCaster;
        }
        
        @Override
        public CasterContext upcast(CasterContext context, Object o) throws BaseException {
            Ref ref=classDef.enableObjectRefs()?context.get(o):null;
            if(ref!=null) {
                CasterContext child=context.getChildContext(o,ref.n);
                child.update(ref);
                return child;
            }
            AccessibleObject instance=c.newInstance(context).makeAccessible(access);
            CasterContext child=context.getChildContext(o,instance.getObject());
            if(classDef.enableObjectRefs()) {
                context.put(o,ref=new Ref(o,child.n,child.mode));
            }
            Caster[] fieldCaster=resolveFieldCasters(context);
            Object[] f=new Object[fieldCaster.length];
            Field[] fields=classDef.getFields();
            for(int i=0;i<f.length;i++) {
                Object t=access.getField(o, fields[i]);
                CasterContext cc=fieldCaster[i].upcast(child, t);
                f[i]=cc.n;
                child.update(cc);
            }
            if(child.finish(ref)!=CastMode.Unmodified) {
                //System.out.println("Object "+o.getClass()+" was upcasted to "+n.getClass()+" ("+child.mode+", conditions="+child.getConditions()+")");
                for(int i=0;i<f.length;i++) {
                    access.setField(instance.getObject(), fields[i], f[i]);
                }
                Object finished=instance.getAssignable();
                if(ref!=null&&finished!=child.n) {
                    throw new BaseException(BaseException.FORBIDDEN,"Reference object of type "+access.getType().getJavaClassType()+" changed.");
                }
                child.n=finished;
            }
            return child;
        }
    }
    
    private class CasterContext implements AccessContext {
        CasterContext child;
        Map<Object,Ref> referenced;
        Object o,n;
        Context context;
        CasterContext parent;
        Set<Ref> conditions;
        CastMode mode;
        private CasterContext(Context context) {
            this.context=context;
            this.referenced=new IdentityHashMap<>();
        }
        private CasterContext(CasterContext parent) {
            this.context=parent.getContext();
            this.parent=parent;
            this.referenced=parent.referenced;
        }
        
        public void addConditions(Set<Ref> conditions) {
            if(conditions!=null&&mode==CastMode.ConditionallyModified) {
                if(this.conditions==null) {
                    this.conditions=new HashSet<DefaultAccessFactory.Ref>();
                }
                this.conditions.addAll(conditions);
            }
        }
        
        public Set<Ref> getConditions() {
            return mode==CastMode.ConditionallyModified?conditions:null;
        }
        
        public CastMode upcasted() {
            return mode;
        }
        
        public Ref get(Object o) {
            return referenced.get(o);
        }
        
        public void update(CasterContext child) {
            switch(child.mode) {
                case Modified:mode=CastMode.Modified;
                    break;
                case ConditionallyModified:if(mode==CastMode.ConditionallyModified) {
                        addConditions(child.getConditions());
                    }
                    break;
            }
        }

        public void update(Ref ref) {
            switch(ref.mode) {
                case Modified:mode=CastMode.Modified;
                    break;
                case ConditionallyModified:if(mode==CastMode.ConditionallyModified) {
                        addConditions(ref.conditions);
                    }
                    break;
            }
        }

        public CasterContext getChildContext(Object o,Object n) {
            if(child==null) {
                child=new CasterContext(this);
            }
            child.o=o;
            child.n=n;
            if(o==n) {
                child.mode=CastMode.Unmodified;
            } else if(o.getClass().equals(n.getClass())) {
                child.mode=CastMode.ConditionallyModified;
                if(child.conditions!=null) {
                    child.conditions.clear();
                }
            } else {
                child.mode=CastMode.Modified;
            }
            return child;
        }
        
        public void put(Object k,Ref ref) {
            referenced.put(k, ref);
        }
        
        @Override
        public <T> T castTo(Context context, Class<T> clazz) {
            T t=context.cast(clazz,o);
            return t==null?(parent==null?context.castTo(clazz):parent.castTo(context,clazz)):t;
        }

        @Override
        public Access resolve(Context context, TypeDefinition type) {
            return DefaultAccessFactory.this.resolve(context, type);
        }

        @Override
        public Constructor resolve(Context context, Type type) {
            return DefaultAccessFactory.this.resolve(context, type);
        }

        @Override
        public Access resolve(Access referrer, TypeDefinition type) {
            return DefaultAccessFactory.this.resolve(referrer, type);
        }

        @Override
        public Context getContext() {
            return context;
        }
        
        public CastMode finish(Ref ref) {
            if(ref!=null&&conditions!=null) {
                conditions.remove(ref);
            }
            if(mode==CastMode.ConditionallyModified&&(conditions==null||conditions.size()==0)) {
                mode=CastMode.Unmodified;
                n=o;
            }
            if(ref!=null) {       
                ref.mode=mode;
                ref.n=n;
                if(mode==CastMode.ConditionallyModified) {
                    ref.conditions=new HashSet<DefaultAccessFactory.Ref>(conditions);
                }
            }
            return mode;
        }
    }
    
    private static class Ref {
        private Object o;
        private Object n;
        private CastMode mode;
        private Set<Ref> conditions;
        private Ref(Object o,Object n,CastMode mode) {
            this.o=o;
            this.n=n;
            this.mode=mode;
            if(mode==CastMode.ConditionallyModified) {
                conditions=new HashSet<>();
                conditions.add(this);
            }
        }
        
        public CastMode upcasted() {
            return mode;//o!=null&&!o.getClass().equals(n.getClass());
        }
    }
    
    private static enum CastMode {
        Unmodified,
        ConditionallyModified,
        Modified;
        
        public CastMode update(CastMode mode) {
            switch(this) {
                case Unmodified:return mode;
                case Modified:return this;
                case ConditionallyModified:switch(mode) {
                    case Unmodified:return this;
                    case Modified:return mode;
                    case ConditionallyModified:return this;
                }
            }
            return this;
        }
    }
}
