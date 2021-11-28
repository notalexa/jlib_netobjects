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
package not.alexa.netobjects.types;

import java.lang.reflect.Method;

import not.alexa.netobjects.BaseException;
import not.alexa.netobjects.Context;
import not.alexa.netobjects.types.ClassTypeDefinition.Field;
import not.alexa.netobjects.types.JavaClass.Type;
import not.alexa.netobjects.types.access.AbstractClassAccess;
import not.alexa.netobjects.types.access.Access;
import not.alexa.netobjects.types.access.AccessFactory;
import not.alexa.netobjects.types.access.Constructor;
import not.alexa.netobjects.utils.TypeUtils;

/**
 * This class represents a functional call. Since the class has a type description the class is a (non final) network object and
 * therefore represents a <b>remote functional call</b>. In contrast to "normal" RMI invocation the original object on which
 * the function is evaluated is located at the caller and not on the remote server implementing <i>true object orientation on
 * the functional call</i>.
 * <br>In general, the result of a functional call depends on the following:
 * <ul>
 * <li>The method (typically described by a name (of type {@link ObjectType} in this context)
 * <li>The object on which the method is executed (typically denoted as <i>this</i> or <i>self</i> and denoted as <i>self</i> in this context)
 * <li>The arguments to the object (denoted as <i>args</i> in this context)
 * <li><i>The current execution enviroment</i>. Not as obvious as the three items above the execution environment is essential for performing
 * the call. For example, the environment may <b>deny execution due to security restrictions</b>.
 * <br>This class represents the execution environment of the call. Since the class is not final the execution environment may change due to 
 * different classes inherited from this class (see the discussion below).
 * </ul>
 * One of the major use cases of this class is the notation of <b>indirect functional calls</b>. In this case, the original call of a method 
 * redirects to an instance of this class which delegates the call to the (upcasted) object back. At first glance the approach seems to be useless
 * but the indirect call can be used introduce additional functionality:
 * <ul>
 * <li>The upcasted object provides "real" functionality by <b>overriding</b> the original method.
 * <li>An upcasted lambda implements a <b>remote functional call</b> implementing distributed method invocations.
 * </ul>
 * Combining the two aspects, it's a matter of deployment where (network) methods are efficiently implemented: Either locally (using an upcast of the API class)
 * or remotely (using a network call to a server (where the API class is upcasted to real implementation)). This approach can be formulated as follows:
 * Suppose {@code A} is a class (or interface) and the method {@code m} should be a method distributable over the network. In this case, the call can be
 * declared as follows:
 * <pre>
 * {@literal @}NetworkObject
 * public [default] Object m(Context context,Object...args) throws BaseException {
 *    return new Lambda(this,args){}.call(context);
 * }
 * </pre>
 * Note that this class is
 * <ul>
 * <li>locally extended (due to technical reasons to reflect the calling method).
 * <li>Has as first argument the context to use for execution.
 * <li>declares to throw a {@link BaseException} as a general exception (used by potential remote method invocation engines to throw network exceptions for example).
 * </ul>
 * 
 * @author notalexa
 *
 */
public class Lambda {
    private static ThreadLocal<Lambda> called=new ThreadLocal<>();
    /**
     * 
     * @return the (class) type definition of this class
     */
    public static ClassTypeDefinition getTypeDescription() {
        return Types.LAMBDA;
    }

    protected ObjectType method;
    protected Object self;
    protected Object[] args;
    protected Lambda call=this;
    private Method m;
    private boolean callService;
    
    public Lambda() {
    }
    
    /**
     * Constructor for extensions.
     * 
     * @param self the {@code this} object of this lambda
     * @param args the arguments of the call
     * @throws RuntimeException if this class is not an inner class of a method, that is
     * if the method {@code getClass().getEnclosingMethod()} returns {@code null}.
     */
    protected Lambda(Object self,Object...args) {
        this.self=self;
        this.args=args;
        m=getClass().getEnclosingMethod();
        if(m!=null) {
            Lambda current=called.get();
            if(current!=null&&current.self==self&&arrayEquals(current.args,args)&&m.equals(current.m)) {
                // Same method same self and same args as former call.
                StackTraceElement[] stack=new Exception().getStackTrace();
                int c=0;
                for(int i=0;i<stack.length;i++) {
                    if(stack[i].getMethodName().equals(m.getName())) {
                        c++;
                    } else if(c>0&&stack[i].getClassName().equals(Lambda.class.getName())) {
                        break;
                    }
                }
                if(c==1) {
                    call=current;
                    callService=true;
                    return;
                }
            }
            call=new Lambda(self,Namespace.getJavaNamespace().createMethodType(m),args);
        } else {
            throw new RuntimeException("Illegal usage of Lambda(Object self,Object...args): Not inside a method");
        }
    }

    /**
     * Represents a method call.
     * 
     * @param self the {@code this} object of this lambda
     * @param method the object type of the method
     * @param args the arguments of the call
     */
    public Lambda(Object self,ObjectType method,Object...args) {
        this.method=method;
        this.self=self;
        this.args=args.length==0?null:args;
    }
    
    /**
     * Prepare the calling lambda. The default implementation upcast the lambda.
     * 
     * @param context the (calling) context for preparation
     * @return a prepared lambda (used for the call)
     */
    protected Lambda prepareCall(Context context) {
        return context.upcast(call);
    }
    
    /**
     * Should we call the service or the method?
     * 
     * @param context the calling context
     * @return {@code true} if the service should be called.
     */
    protected boolean callService(Context context) {
        return callService;
    }
    
    private boolean arrayEquals(Object[] a,Object[] b) {
        if(a==null) {
            return b==null;
        }
        if(a.length==b.length) {
            for(int i=0;i<a.length;i++) {
                if(a[i]!=b[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    
    /**
     * Main method of this class. The method 
     * @param <T>
     * @param context
     * @return
     * @throws BaseException
     */
    public final <T> T call(Context context) throws BaseException {
        if(callService(context)) {
            return call.invokeService(context,false);
        }
        Method method=this.m;
        if(method==null) {
            Type m=TypeUtils.getType(context,this.method);
            method=m==null?null:context.getTypeLoader().getLinkedMethod(m);
        }
        if(method==null) {
            return call.invokeService(context,true);
        } else {
            Lambda l=prepareCall(context);
            if(l.callService(context)) {
                return l.invokeService(context, false);
            } else try {
                // We have already resolved the method
                l.m=method;
                // Save this in case of a service call
                called.set(l);
                return l.invokeMethod(context);
            } catch(Throwable t) {
                return BaseException.throwException(t);
            } finally {
                called.remove();
            }
        }
    }
    
    /**
     * Method to invoke an (external) service. This method is called in case of a second call
     * (with the same arguments) to the {@link #call(Context)} method.
     * <br>The default implementation just throws an error.
     * 
     * @param <T> the return type
     * @param context the context to use for the service call
     * @param implicit if {@code true} the service was called because no method was resolved.
     * @return the result of the service call
     * @throws BaseException if an error occurs
     */
    protected <T> T invokeService(Context context,boolean implicit) throws BaseException {
        throw new BaseException(BaseException.BAD_REQUEST,"Not locally linked: "+this.method);
    }
    
    /**
     * Method to invoke the resolved method. This method is called in case of the first
     * call to {@link #call(Context)}. In case {@code self} is overloaded and the overloaded
     * class redefines the method this overloaded method is called. In all other cases
     * the original method is called (again) resulting in a service call as the second call
     * to {@link #call(Context)}.
     * 
     * @param <T> the return type
     * @param context the context to use for the method call
     * @return the result of the method call
     * @throws BaseException if an error occurs
     * @throws Throwable if an internal error occurs
     */
    protected <T> T invokeMethod(Context context) throws BaseException, Throwable {
        Object[] allArgs=args==null?new Object[] { context}:new Object[args.length+1];
        if(args!=null) {
            allArgs[0]=context;
            System.arraycopy(args,0, allArgs, 1, args.length);
        }
        return (T)m.invoke(self, allArgs);
    }
    
    /**
     * Class access for a lambda
     * 
     * @author notalexa
     *
     */
    public static class ClassAccess extends AbstractClassAccess implements Access {
        public ClassAccess(AccessFactory factory,Constructor c) {
            super(factory,getTypeDescription(),c);
        }
        
        @Override
        public Object getField(Object o, int index) throws BaseException {
            Lambda l=(Lambda)o;
            switch(index) {
                case 0:return l.method;
                case 1:return l.self;
                case 2:return l.args==null||l.args.length==0?null:l.args;
            }
            return null;
        }

        @Override
        public void setField(Object o, int index, Object v) throws BaseException {
            Lambda l=(Lambda)o;
            switch(index) {
                case 0:l.method=(ObjectType)v;
                    break;
                case 1:l.self=v;
                    break;
                case 2:l.args=(Object[])v;
                    break;
            }
        }

        @Override
        public Access createFieldAccess(Field f) throws BaseException {
            switch(f.getIndex()) {
                case 2:return forArray(f.getType(),Object[].class);
            }
            return super.createFieldAccess(f);
        }
    }
}
