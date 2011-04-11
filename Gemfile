source :rubygems

gem 'rack', '~> 1.2.2'
gem 'rails', '~> 3.0.6'
gem 'json', '~> 1.5.1'
gem 'plucky', '~> 0.3.6'
gem 'chronic', '~> 0.3.0'
gem 'pony', '~> 1.1'  # unusual version number
gem 'graylog2-declarative_authorization', :require => 'declarative_authorization'
gem 'mongoid', '~> 2.0.1'
gem 'bson_ext', "~> 1.3.0"

# TODO https://github.com/ph7/system-timer/issues/15
if RUBY_VERSION.start_with?('1.8')
  gem 'SystemTimer', '~> 1.2.3'
end

group :development, :test do
  # might be useful to generate fake data in development
  gem 'machinist_mongo', '~> 1.2.0', :require => 'machinist/mongoid'
  gem 'faker', '~> 0.9.5'
end

group :test do
  gem 'ci_reporter'
  gem 'shoulda', '~> 2.11.3'
  gem 'shoulda-activemodel', '0.0.2', :require => 'shoulda/active_model'  # fixed version - too hacky
  gem 'mocha', '~> 0.9.12'
  gem 'database_cleaner', '~> 0.6.0'
end
