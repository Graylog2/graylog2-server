New Relic RPM
=============

New Relic RPM is a Ruby performance management system, developed by
[New Relic, Inc](http://www.newrelic.com).  RPM provides you with deep
information about the performance of your Ruby on Rails or Merb
application as it runs in production. The New Relic Agent is
dual-purposed as a either a Rails plugin or a Gem, hosted on
[github](http://github.com/newrelic/rpm/tree/master) and Rubyforge.

The New Relic Agent runs in one of two modes:

**Developer Mode** : Adds a web interface mapped to /newrelic to your
  application for showing detailed performance metrics on a page by
  page basis.
  
**Production Mode** : Low overhead instrumentation that captures
  detailed information on your application running in production and
  transmits them to rpm.newrelic.com where you can monitor them in
  real time.

### Supported Environments

* Ruby 1.8.6, 1.8.7 or 1.9.1
* JRuby
* Rails 1.2.6 or above
* Merb 1.0 or above

Developer Mode
--------------

Developer mode is on by default when you run your application in the
development environment (but not when it runs in other environments.)
When running in developer mode, RPM will track the performance of
every http request serviced by your application, and store in memory
this information for the last 100 http transactions.

When running in Developer Mode, the RPM will also add a few pages to
your application that allow you to analyze this performance
information. (Don't worry--those pages are not added to your
application's routes when you run in production mode.)

To view this performance information, including detailed SQL statement
analysis, open `/newrelic` in your web application.  For instance if
you are running mongrel or thin on port 3000, enter the following into
your browser:

    http://localhost:3000/newrelic

Production Mode
---------------

When your application runs in the production environment, the New
Relic agent runs in production mode. It connects to the New Relic RPM
service and sends deep performance data to the RPM service for your
analysis. To view this data, login to
[http://rpm.newrelic.com](http://rpm.newrelic.com).

NOTE: You must have a valid account and license key to view this data
online.  Refer to instructions in *Getting Started*, below.

Getting Started
===============

RPM requires an agent be installed in the application as either a
Rails plug-in or a gem.  Both are available on RubyForge--instructions
below.

To use Developer Mode, simply install the gem or plugin into your
application and follow the instructions below.

To monitor your applications in production, create an account at
[www.newrelic.com](http://newrelic.com/get-RPM.html).  There you can
sign up for a free Lite account or one of our paid subscriptions.

Once you receive the welcome e-mail with a license key and
`newrelic.yml` file, copy the `newrelic.yml` file into your app config
directory.

### Rails Plug-In Installation

    script/plugin install http://newrelic.rubyforge.org/svn/newrelic_rpm
   
### Gem Installation

    sudo gem install newrelic_rpm

For Rails, edit `environment.rb` and add to the initalizer block:

    config.gem "newrelic_rpm" 

The Developer Mode is unavailable when using the gem on Rails versions
prior to 2.0.

### Merb Support

To monitor a merb app install the newrelic_rpm gem and add

    dependency 'newrelic_rpm'

to your init.rb file.

Current features implemented:

* Standard monitoring, overview pages
* Error capturing
* Full Active Record instrumentation, including SQL explains
* Very limited Data Mapper instrumentation
* Transaction Traces are implemented but will not be very useful
  with Data Mapper until more work is done with the Data Mapper
  instrumentation

Still under development:

* Developer Mode
* Data Mapper bindings

### Github

The agent is also available on Github under newrelic/rpm.  Fork away!

### Support

Reach out to us--and to fellow RPM users--at
[support.newrelic.com](http://support.newrelic.com/discussions/support).
There you'll find documentation, FAQs, and forums where you can submit
suggestions and discuss RPM with New Relic staff and other users.

Find a bug?  E-mail support@newrelic.com, or post it to
[support.newrelic.com](http://support.newrelic.com/discussions/support).

Refer to [our website](http://www.newrelic.com/support) for other
support channels.

Thank you, and may your application scale to infinity plus one.  

Lew Cirne, Founder and CEO<br/>
New Relic, Inc.
