source :rubygems

gem 'rack', '~> 1.2.2'
gem 'rails', '~> 3.0.7'
gem 'json', '~> 1.5.1'
gem 'plucky', '~> 0.3.6'
if RUBY_PLATFORM !~ /java/
  gem 'home_run', '~> 1.0.2'
end
gem 'chronic', '~> 0.3.0'
gem 'pony', '~> 1.1'  # unusual version number
gem 'graylog2-declarative_authorization', :require => 'declarative_authorization'
gem 'hoptoad_notifier', '~> 2.4.9'
gem 'newrelic_rpm', '~> 3.0.0', :require => nil  # loaded by rpm_contrib
gem 'rpm_contrib', '~> 2.1.0'
gem 'mongoid', '~> 2.0.1'
if RUBY_PLATFORM =~ /java/
  gem 'bson', "~> 1.3.1"
else
  gem 'bson_ext', "~> 1.3.1"
end

# TODO https://github.com/ph7/system-timer/issues/15
if RUBY_VERSION.start_with?('1.8') && RUBY_PLATFORM !~ /java/
  gem 'SystemTimer', '~> 1.2.3'
end

group :development, :test do
  # might be useful to generate fake data in development
  gem 'machinist_mongo', '~> 1.2.0', :require => 'machinist/mongoid'
  gem 'faker', '~> 0.9.5'
end

group :development do
  # gem 'ruby-prof', '~> 0.10.5'  # works nice with NewRelic RPM Developer Mode
end

group :test do
  gem 'ci_reporter'
  gem 'shoulda', '~> 2.11.3'
  gem 'shoulda-activemodel', '0.0.2', :require => 'shoulda/active_model'  # fixed version - too hacky
  gem 'mocha', '~> 0.9.12'
  gem 'database_cleaner', '~> 0.6.0'
  gem 'timecop', '~> 0.3.5'
end
