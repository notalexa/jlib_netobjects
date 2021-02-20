# Network Objects

Objects needs a place to live. In the simplest case, objects are
located locally inside memory and consist of data and executable
code. A second example are objects which are located remotely. Both data and executable code are located on a remote machine.
Therefore, from the viewpoint of the remote machine, this objects are itself located locally. Typically a client use a
lookup mechanism to call methods on this remote object. This lookup can be viewed as a primary key to the object itself. Examples for lookups are (HTTP-)URL's or RMI context lookups.
Parameters of (and answers from) method calls needs to travel across the network and therefore needs to be serializable but the object itself not.

Network objects are different. They live in the network itself and can travel to other machines in the same network for methods evaluation always choosing *the best place* for the method call. Return values of the call (and side effects on the object itself if any) travel back to the original location of course and can be used there.
Therefore, network objects needs to be serializable itself. This implies that network objects are storable and consequently, network objects can move **in space and time**. This observation also implies that configurations based on network objects do not need any other mechanisms to create objects. They are *just there*.

To realize the approach, data (and the method signatures) and the implementation of methods are divided into the definition of a network type and a local method implementation. The process of attaching a (local) implementation of methods whenever a network object deserializes on a given machine is called a local linkage. For example, a network type may be locally linked to a Java class. Deserializing the network object inside the VM links the network object to a normal Java Object and additionally implements all methods of the network type by corresponding java types. Note that the attachment of Java Classes to a network type is not unique. Different classes (with different method implementations) may be linked locally to the same network type on different machines providing different implementations of the global method. Beside this, network types have the following general properties:

* Network types are itself Network objects. Therefore, types can move across the network (and can be saved as mentioned above).
* Network types are an abstract layer. No assumptions are made on how the objects are serialized. The library provides currently a base implementation on serializing to XML but in future, other mechanisms may be provided to serialize network objects (or subsets of) including
<ul>
<li>JSON
<li>YAML
<li>protobuf
<li>Android's Parcelable
</ul>
* Network types are divided into two categories: Anonymous types and named types. Anonymous types do not have a specific name and can be viewed as generics. Examples are arrays and map entries. Named types have "names" identifiying the type. Each name is associated to a specific "namespace" in which the name is assumed to be unique. No assumption on the namespace is done and any type can have a name in multiple namespaces. Currently, the only namespace provided is the Java Class Namespace in which the name of a network type is a Java Class Name.
* The library introduce a set of "primitive types" which is closely related to Java's primitive types but exceeds them (by string, uuid, large numbers for example). Each primitive type **must** be named in every namespace introduced.
* The library introduce a "method type" defining the notation of methods. (Network) objects of this type are "callable". Invoking a call on an object of this type triggers execution (possibly on a remote server) of a method implementation somewhere with the provided arguments returning the results. Note that a method doesn't need to be stateless. Typically, the method itself has an underlying (network) type and "contains" an 
object of this type as the **this** object.
<br>Examples of candidates for network objects of this type are:
<ul>
<li>Local method invocation: The object contains the method itself (obtained via reflection for example) and an object of the defining class as
the this object. Invoking the (network) method means calling the local method.
<li>RMI: The object contains the RMI stub and stub method. Invoking the (network) method means calling the stub method (executing effectively on
the server side).
<li>Webservices: The object contains effectively information about the URL of the service. Invoking the (network) method means creating JSON representations of the arguments, creating and executing the HTTP(s) call and deserializing the result from JSON into a Java Object.
<li>Messaging Systems: If the (network) method doesn't provide a result, the method can be executed asynchronously. In this case, the transport
mechanism of the data may be a messaging system.
</ul>

<p>
The first version introduce the definition of network types and basic functionality like encoding/decoding into XML especially for the provided
network types defining the network types itself. Minor versions introduce additional encoding/decoding functionality. In this version, there exist only one possibility to define network types: Programmatically. This is not very comfortable of course. But additional mechanisms can be
considered and will be provided in future versions:

* Using an annotation framework (like JAXB)
* Using IDLs (like AIDL).

Method invocation (even local execution) is not provided in the first version. Any progress on this will result in an increased version number.
  
