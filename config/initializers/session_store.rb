# Be sure to restart your server when you modify this file.

# http://jira.graylog2.org/browse/WEBINTERFACE-80
custom_cookie_name = Configuration.custom_cookie_name
if custom_cookie_name.blank?
  cookie_name = "graylog2_web_interface"
else
  cookie_name = custom_cookie_name
end

Graylog2WebInterface::Application.config.session_store :cookie_store, :key => "_#{cookie_name}_session"

# Use the database for sessions instead of the cookie-based default,
# which shouldn't be used to store highly confidential information
# (create the session table with "rake db:sessions:create")
# Graylog2WebInterface::Application.config.session_store :active_record_store
