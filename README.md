[![Build Status](https://travis-ci.org/Graylog2/graylog2-web-interface.png)](https://travis-ci.org/Graylog2/graylog2-web-interface)

## Development Setup

* Make sure Java 7 and Maven is installed
* Install Play version 2.2.2 and ensure the `play` binary is in `PATH`
* Clone the git repository and initialize the submodules

```
$ git submodule init
Submodule 'modules/graylog2-rest-client' (git://github.com/Graylog2/graylog2-rest-client.git) registered for path 'modules/graylog2-rest-client'

$ git submodule update
Cloning into 'modules/graylog2-rest-client'...
remote: Counting objects: 1227, done.
remote: Compressing objects: 100% (16/16), done.
remote: Total 1227 (delta 3), reused 0 (delta 0)
Receiving objects: 100% (1227/1227), 234.23 KiB | 303.00 KiB/s, done.
Resolving deltas: 100% (671/671), done.
Checking connectivity... done.
Submodule path 'modules/graylog2-rest-client': checked out 'dbd45912656eaf0b8794d7fd6133eeede37f8408'
```

* Run the server

```
$ play run
...

--- (Running the application from SBT, auto-reloading is enabled) ---

[info] play - Listening for HTTP on /0:0:0:0:0:0:0:0:9000

(Server started, use Ctrl+D to stop and go back to the console...)

```
