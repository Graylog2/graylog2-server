# Graylog Web Interface

## Requirements
- [Node.js](https://nodejs.org/), at this time we use v8.9.1
- [Yarn](https://yarnpkg.com/)

**Note:** NPM v5 changed completely the way it builds local modules, breaking the Graylog web interface build. Please use Yarn instead of NPM v5.

## Development Setup

* Install the requirements listed above
* Run `yarn install`
* Run `yarn start` to build the project for development and start the development server. You can exclude any Graylog frontend plugins from the build by running `disable_plugins=true npm start` instead
* Open http://localhost:8080 in your browser to access the Graylog web interface

The `yarn start` (or `disable_plugins=true yarn start`) command will run an [Express](http://expressjs.com) web server which is configured to do a full page reload in your browser every time that you save a file. This ensures that you will always use the latest version of your code.

### Run development server in a different host and port

You can start the development server in any other host and port that you like:

- Use the `--host=<hostname>` option to change the default host the development server uses. The default host is `127.0.0.1`
- Use the `--port=<port>` option to change the default port number the development server uses. The default value is `8080`. The server will pick a random port if the port you try to use is already in use

E.g. `yarn start --host=0.0.0.0 --port=8000` will start the development server in all available network interfaces using the port 8000.

## Frontend documentation and component gallery
There's an online version of the frontend documentation and component gallery at:

[https://graylog2.github.io/frontend-documentation/](https://graylog2.github.io/frontend-documentation/)

The online version is automatically deployed and reflects the current state of the `master` branch in this repository.

### Run documentation locally
You may also run the documentation locally to contribute to it or see a different version than the current master:

1. Run `yarn install`
1. Run `yarn run docs:server`
1. Go to [http://localhost:6060](http://localhost:6060) on your favourite browser to see the local documentation

## Configure your editor

We mainly develop using IntelliJ or WebStorm. If you also decide to use them to work in Graylog, enable `React JSX` as Javascript language version to support the JSX language extension. This setting was called `JSX harmony` before, and it is available in one or the other form since IntelliJ 14 and WebStorm 9.

## Update Javascript dependencies

1. Update a single dependency

    * Run `yarn upgrade <package>@<version>`
    * Commit any changes in both `package.json` and `yarn.lock` files
    * Do any changes required to adapt the code to the upgraded modules

1. Update many dependencies

    * It may be dangerous updating many dependencies at the same time, so be sure you checked the upgrade notes of all modules before getting started. Once you are ready to upgrade the modules, Yarn provides a few options to do it:
        * You can pass all packages you want to upgrade to Yarn: `yarn upgrade <package1> <package2>...`
        * Yarn also supports upgrading packages matching a pattern, so you can execute `yarn upgrade --pattern <pattern>`
        * You could execute `yarn upgrade` if you really want to upgrade all packages
    * After doing the upgrade, remember to commit both the `package.json` and `yarn.lock` files
