Getting started with your new Graylog plugin
============================================

Welcome to your new Graylog plugin!

Please refer to http://docs.graylog.org/en/latest/pages/plugins.html for documentation on how to write
plugins for Graylog.

Travis CI
---------

There is a `.travis.yml` template in this project which is prepared to automatically
deploy the plugin artifacts (JAR, DEB, RPM) to GitHub releases.

You just have to add your encrypted GitHub access token to the `.travis.yml`.
The token can be generated in your [GitHub personal access token settings](https://github.com/settings/tokens).

Before Travis CI works, you have to enable it. Install the Travis CI command line
application and execute `travis enable`.

To encrypt your GitHub access token you can use `travis encrypt`.

Alternatively you can use `travis setup -f releases` to automatically create a GitHub
access token and add it to the `.travis.yml` file. **Attention:** doing this
will replace some parts of the `.travis.yml` file and you have to restore previous
settings.
