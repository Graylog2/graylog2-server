# Be sure to restart your server when you modify this file.

# Your secret key for verifying cookie session data integrity.
# If you change this key, all old sessions will become invalid!
# Make sure the secret is at least 30 characters and all random, 
# no regular words or you'll be exposed to dictionary attacks.
ActionController::Base.session = {
  :key         => '_flot_test_session',
  :secret      => 'b0b3ef3e1e76a9f899eb40e912bd689f6da51b22d1c967db23d37426ab0fcdee470472fa62ac30c69867cec238b4f9f1afb1856e1eb695f01bef1e646d446a96'
}

# Use the database for sessions instead of the cookie-based default,
# which shouldn't be used to store highly confidential information
# (create the session table with "rake db:sessions:create")
# ActionController::Base.session_store = :active_record_store
