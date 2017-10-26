# Graylog Web Interface

## Development Setup

* Install [node.js](http://nodejs.org/) and npm.
* Run `npm install`
* Run `npm start` (if you don't want to include plugins in the build while developing, simply run `disable_plugins=true npm start`) 
* Open http://localhost:8080

The `npm start` (or `disable_plugins=true npm start`) command will run the `webpack-dev-server`, which allows in-browser hot reloading.
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

### Alternative Development Setup

Due to problems with webpack-dev-server there is another way to run the development setup.

* Install [devd](https://github.com/cortesi/devd)
* Install [node.js](http://nodejs.org/) and npm.
* Run `npm install`
* Run `npm run watch` and **keep it running** to start webpack in watch mode so it rebuilds on source changes
* Run `npm run devd` and **keep it running** once the `build/` directory exists
* Open http://localhost:8080


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