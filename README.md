# BAUProfiler

BAUProfiler is a profiling framework for Java applications based on the profiling requirements of the [TornadoVM](https://github.com/beehive-lab/TornadoVM)

### Requirements
#### Profiler
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

#### JSON plugin
- Generates ConvertableProfiles from user-defined classes that extend Profile and are annotated with supplied JSON annotations
- Assembles the ConvertableProfiles to create expected output
