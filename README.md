#BAUProfiler

BAUProfiler is a profiling framework for Java applications based on the profiling requirements of the [TornadoVM](https://github.com/beehive-lab/TornadoVM)

### Profiler
- Singleton profiler
- Thread-safe profiling
- Handles profiling requests async (variable # of handlers)
- Groups profiling requests using a supplied _groupid_
- Optional cached profiling requests added to a given group (at a later time) or shared between multiple groups
- Profiling call args are instances of user-defined classes that extend ConvertableProfile
- User-defined classes allow for a mix of logging and profiling
- Optional return of timer instance from profiling call, also passed to user-defined class, allowing for information extraction during [iv] (see below)
- User-defined classes implement methods used by the profiler to:
   1. Perform preprocessing (alternative to processing in constructor)
   2. Invalidate requests
   3. Order requests based on dependencies
   4. Finalize field values prior to conversion to the desired output format
- Provides a synchronisation point, prior to which the user-defined class can be externally (directly by the user) or internally (using methods [i] or [iv] above) modified, and after which the final conversion to the desired output format occurs. 
- Pluggable output format with JSON plugin provided

### JSON plugin
- Generates ConvertableProfiles from user-defined classes that extend Profile and are annotated with supplied JSON annotations
- Assembles the ConvertableProfiles to create expected output

### Examples
For the customary Hello World, let's work backward from the expected JSON output:
```json
{
  "message": "Hello World!"
}
```
This requires the scaffolding for the `message` key, which takes the form of a user-defined class that implements `Profile` and is annotated with JSON specific annotations.
```java
@JClass
public class Message implements Profile {
  @JField protected final String message;

  public Message(String message) {
    this.message = message;
  }
}
```
All of `Profile`'s methods have defaults and do not need implementing if not required. The JSON annotations are processed and the information is used to generate a sub-class of `Message`(in this case) named `GeneratedMessage` that will be used to make the profiling calls.
```java
Profiler profiler = Profiler.getInstance();
long GID = 1;

profiler.attach(GID);
profiler.profile(new GeneratedMessage("Hello World!"), GID);
profiler.detach(GID);
```
Each JSON string is associated with a group id. The `attach` method notifies the Profiler to begin a new JSON string and to associate it with the given group id. The `detach` method marks the completion of the JSON string associated with the given group id and releases the group id for reuse with a new string.

You're probably wondering how the `@JClass` and `@JField` annotations can be used to generate more complex JSON. Let's find out by adding a little depth to our expected output.
```json
{
  "examples": {
    "helloWorld": {
      "message": "Hello World!"
    }
  }
}
```
To generate this output, all we need to do is specify the value of the `path` element defined by the `@JClass` annotation.
```java
@JClass(path="examples/helloWorld")
public class Message implements Profile {
  // same as before...
}
```

Now let's slowly start adding more information to our JSON output:
```json
{
  "examples": {
    "helloWorld": {
      "msg_id": 1,
      "message": "Hello World!"
    }
  }
}
```
Modifying our `Message` class as such:
```java
@JClass(path="examples/helloWorld")
public class Message implements Profile {
  private static int globalId = 1;
  @JField(prefix="msg_") protected final int id;
  @JField protected final String message;

  public Message(String message) {
    this.message = message;
    this.id = globalId++;
  }
}
```
There are a few additions here, starting with the `id` instance variable. Only the instance variables annotated with `@JField` are included in the JSON string as key value pairs, and they are included in the same order as they are defined. By default, the name of an instance variable is used as the key in the JSON string. `@JField` provides `prefix`, `postfix` and `key` elements as modifier for the key name.

Now let's convert the "helloWorld" object into an object array by modifying the `@JClass` annotation:
```java
@JClass(path="examples/helloWorld", type=JType.OBJECT_ARRAY)
```
The output becomes:
```json
{
	"examples": {
		"helloWorld": [{
			"msg_id": 1,
			"message": "Hello World!"
		}]
	}
}
```
We can now accumulate messages in the object array with extra profiling calls like
```profiler.profile(new GeneratedMessage("Hello again World!"), GID)```
```json
{
	"examples": {
		"helloWorld": [{
			"msg_id": 1,
			"message": "Hello World!"
		}, {
			"msg_id": 2,
			"message": "Hello again World!"
		}]
	}
}
```

The user-defined profiles (i.e. extensions of `Profile`) are assembled by the profiler in the order they are received. ORDER MATTERS.
Let's break something. Defining another user-defined profile:
```java
@JClass(path="examples/endOfWorld")
public class EndOfWorld implements Profile {
  @JField protected final String message;

  public EndOfWorld(String message) {
    this.message = message;
  }
}
```
And inserting between the two previous profiling class
```java
profiler.profile(new GeneratedMessage("Hello World!"), GID);
profiler.profile(new GeneratedEndOfWorld("Oops! I broke your json... YOUR BAD?"), GID);
profiler.profile(new GeneratedMessage("Hello again World!"), GID);
```
The JSON output
```json
{
	"examples": {
		"helloWorld": [{
			"msg_id": 1,
			"message": "Hello World!"
		}],
		"endOfWorld": {
			"message": "Oops! I broke your json... YOUR BAD?"
		},
		"helloWorld": [{
			"msg_id": 2,
			"message": "Hello again World!"
		}]
	}
}
```
Our linter says ```SyntaxError: Duplicate key 'helloWorld' on line 10```. Again, ORDER MATTERS. However, chaos reins so we have a dependency system.
```java
@JClass(path="examples/helloWorld", type=JType.OBJECT_ARRAY)
public class Message implements Profile, Dependency<Message> {
  // same as before...
  public boolean predicate(Message dep) {
  	return true;
  }
}
```
By implementing the generic `Dependency` interface, and self referencing in the type parameter, we create a recursive dependency that reorders all the profiling requests made with an instance of `GeneratedMessage`, sequentially. ORDER MATTERS BUT WE CAN REORDER.
```json
{
	"examples": {
		"helloWorld": [{
			"msg_id": 1,
			"message": "Hello World!"
		}, {
			"msg_id": 2,
			"message": "Hello again World!"
		}],
		"endOfWorld": {
			"message": "Ahhh! You fixed me!"
		}
	}
}
```
The dependency system helps in the creation of structured json strings, independent of the order of the profiling calls.
