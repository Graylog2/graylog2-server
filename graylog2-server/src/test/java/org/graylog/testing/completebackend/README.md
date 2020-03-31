# Environment Variables

The following environment variables can be set to configure local execution:
- GRAYLOG_IT_SKIP_PACKAGING
  
  Setting this to "true" (ignoring case) will skip packaging the server jar. 
  
  This is useful to speed up execution when you are working on tests and are sure that the code under test is not changing.
  
- GRAYLOG_IT_DEBUG_SERVER

  Setting this to "true" (ignoring case) will allow debugging the code under test running inside a container.

# Local Execution

Running `mvn verify` locally should execute the tests in the same way as on a CI server. 
The system will use the Jars it finds in the `target` directories, because it's assumed that up-to-date ones have been built during the `package` phase.

Executing tests from outside Maven (e.g., from IntelliJ) will explicitly trigger and wait for a `mvn package` to ensure that up-to-date Jars are available. 
This packaging can be disabled by setting the env var GRAYLOG_IT_SKIP_PACKAGING (see above).

*Note: For this to work the `mvn` executable file must be in your `PATH` and its path must not contain the `~` character.* 

# Debugging

Debugging can be enabled by setting the env var GRAYLOG_IT_DEBUG_SERVER (see above). 

Attaching the debugger requires a manual step, because the debug port will be mapped dynamically.
A breakpoint can be set in the beginning of a test to interrupt the execution, copy the logged debug port
and a start a remote debugging session using that port.
