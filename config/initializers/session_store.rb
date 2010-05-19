# Be sure to restart your server when you modify this file.

# Your secret key for verifying cookie session data integrity.
# If you change this key, all old sessions will become invalid!
# Make sure the secret is at least 30 characters and all random, 
# no regular words or you'll be exposed to dictionary attacks.
ActionController::Base.session = {
  :key         => '_gs-web_session',
  :secret      => '842106bfb211dd972dc48786fc66cf670ef76a1a5000898bd1db774ee68627f4a05a61db05f7d3131401e00d1bc18a1f01a8693ed83305aad235880527a0902b'
}

# Use the database for sessions instead of the cookie-based default,
# which shouldn't be used to store highly confidential information
# (create the session table with "rake db:sessions:create")
# ActionController::Base.session_store = :active_record_store
