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
package not.alexa.netobjects.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import not.alexa.netobjects.types.JavaClass.Type;
import not.alexa.netobjects.types.ObjectType;
import not.alexa.netobjects.types.TypeDefinition;
import not.alexa.netobjects.types.TypeLoader;

/**
 * Type loader which implements overlay support. All 
 * @author notalexa
 *
 */
public class OverlayTypeLoader implements TypeLoader {
    private TypeLoader parent;
    private Map<Type, Type.InstanceSupport> overlays;

    public OverlayTypeLoader(TypeLoader parent,Collection<Class<?>> additionalOverlays) {
        this(parent,null,additionalOverlays);
    }

    private OverlayTypeLoader(TypeLoader parent,Map<Type,Type.InstanceSupport> preloaded,Collection<Class<?>> additionalOverlays) {
        this.parent=parent;
        if(preloaded!=null) {
            overlays=new HashMap<Type, Type.InstanceSupport>(preloaded);
        }
        for(Class<?> overlay:additionalOverlays) {
            defineOverlay(overlay);
        }
    }

    public ClassLoader getClassLoader() {
        return parent.getClassLoader();
    }
    
    public TypeDefinition resolveType(ObjectType t) {
        return parent.resolveType(t);
    }
    
    @Override
    public Class<?> getLinkedLocal(Type type) {
        if(type!=null) {
            if(type.hasOverlays()&&overlays!=null) {
                Type.InstanceSupport o=overlays.get(type);
                if(o!=null) {
                    return o.getOverlayClass();
                }
            }
        }
        return parent.getLinkedLocal(type);
    }
    
    public TypeLoader overlay(Class<?>... overlays) {
        return overlay(Arrays.asList(overlays));
    }
    
    public TypeLoader overlay(Collection<Class<?>> overlays) {
        return new OverlayTypeLoader(parent, this.overlays,overlays);
    }
    
    private void defineOverlay(Class<?> overlay) {
        try {
            Class<?> overloaded=OverlayUtils.resolve(overlay);
            if(!overloaded.equals(overlay)) {
                Type type=ObjectType.createClassType(overloaded);
                if(overlays==null) {
                    overlays=new HashMap<Type, Type.InstanceSupport>();
                }
                overlays.put(ObjectType.createClassType(overloaded), type.createInstanceSupport(getClassLoader(),overlay));
            }
        } catch(Throwable t) {
        }
    }        
}
