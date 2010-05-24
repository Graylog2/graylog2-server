# == New Relic Initialization
# 
# When installed as a gem, you can activate the New Relic agent one of the following ways:
#
# For Rails, add:
#    config.gem 'newrelic_rpm'
# to your initialization sequence.
#
# For merb, do 
#    dependency 'newrelic_rpm'
# in the Merb config/init.rb
#
# For Sinatra, do
#    require 'newrelic_rpm'
# after requiring 'sinatra'.
#
# For other frameworks, or to manage the agent manually, invoke NewRelic::Agent#manual_start
# directly.
#
require 'new_relic/control'

# After verison 2.0 of Rails we can access the configuration directly.
# We need it to add dev mode routes after initialization finished. 
if defined? Rails.configuration
  Rails.configuration.after_initialize do
    NewRelic::Control.instance.init_plugin :config => Rails.configuration
  end
elsif defined? Merb
  module NewRelic
    class MerbBootLoader < Merb::BootLoader
      after Merb::BootLoader::ChooseAdapter

      def self.run
        NewRelic::Control.instance.init_plugin
      end
    end
  end
else
  NewRelic::Control.instance.init_plugin
end
