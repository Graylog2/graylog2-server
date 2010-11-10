puts "Thanks for installing the Rails upgrade plugin.  This is a set of generators and analysis tools to help you upgrade your application to Rails 3.  It consists of three tasks...

To get a feel for what you'll need to change to get your app running, run the application analysis:

    rake rails:upgrade:check
    
This should give you an idea of the manual changes that need to be done, but you'll probably want to upgrade some of those automatically.  The fastest way to do this is to run 'rails .', which will simply generate a new app on top of your existing code.  But this generation also has the effect of replacing some existing files, some of which you might not want to replace.  To back those up, first run:

    rake rails:upgrade:backup
    
That will backup files you've probably edited that will be replaced in the upgrade; if you finish the upgrade and find that you don't need the old copies, just delete them.  Otherwise, copy their contents back into the new files or run one of the following upgraders...

Routes upgrader
===============

To generate a new routes file from your existing routes file, simply run the following Rake task:

    rake rails:upgrade:routes
    
This will output a new routes file that you can copy and paste or pipe into a new, Rails 3 compatible config/routes.rb.

Gemfile generator
=================  

Creating a new Gemfile is as simple as running:

    rake rails:upgrade:gems
    
This task will extract your config.gem calls and generate code you can put into a bundler compatible Gemfile.

Configuration generator
=======================

Much of the configuration information that lived in environment.rb now belongs in a new file named config/application.rb; use the following task to generate code you can put into config/application.rb from your existing config/environment.rb:

    rake rails:upgrade:configuration

"