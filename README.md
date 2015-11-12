# Graylog Web Interface
[![Build Status](https://travis-ci.org/Graylog2/graylog2-web-interface.png)](https://travis-ci.org/Graylog2/graylog2-web-interface)

## Development Setup

* Install [node.js](http://nodejs.org/) and npm.
* `cd javascript/`
* `npm install`
* `npm start`

* open http://localhost:8080

The `npm start` command will run the `webpack-dev-server`, which allows in-browser hot reloading.
In order to make switching between different branches faster, we use a script to store all `node_modules` folders
into `.node_cache` and then symlink the folder for the current branch to `node_modules`.

When using IntelliJ or WebStorm, be sure to enable `JSX harmony` (available in IntelliJ 14 and WebStorm 9)
as JavaScript language version to properly support react templates.

You might get an error message during `npm install` from `gyp` because the installed (default) Python version is too recent (sic!):

```
gyp ERR! stack Error: Python executable "python" is v3.4.2, which is not supported by gyp.                                                                                                                 
```

In this case just set the correct (installed!) Python binary before running `npm install`:

```
npm config set python python2.7
```

#### Update Javascript dependencies

a. Update a single dependency

* Update `package.json` file with the new dependency
* `npm update <npm-package>`
* `npm shrinkwrap --dev` to save the whole dependency tree into the `npm-shrinkwrap.json` file

b. Update devDependencies

* `npm shinkwrap` to keep the dependency tree (without devDependencies) into `npm-shrinkwrap.json`
* Update `package.json` file with the new devDependencies
* `npm install`
* Do more work with the new devDependencies
* `npm shrinkwrap --dev` to export the whole dependency tree with the new devDependencies into `npm-shrinkwrap.json`

![YourKit](https://s3.amazonaws.com/graylog2public/images/yourkit.png)

YourKit supports our open source project by sponsoring its full-featured Java Profiler. YourKit, LLC is the creator of [YourKit Java Profiler](http://www.yourkit.com/java/profiler/index.jsp) and [YourKit .NET Profiler](http://www.yourkit.com/.net/profiler/index.jsp), innovative and intelligent tools for profiling Java and .NET applications.
