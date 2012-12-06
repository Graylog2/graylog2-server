require 'digest/sha1'
require 'base64'

class User
  include Mongoid::Document
  include Authentication
  include Authentication::ByPassword
  include Authentication::ByCookieToken

  validates_presence_of     :login
  validates_length_of       :login,    :within => 3..40
  validates_uniqueness_of   :login
  validates_format_of       :login,    :with => Authentication.login_regex, :message => Authentication.bad_login_message

  validates_format_of       :name,     :with => Authentication.name_regex,  :message => Authentication.bad_name_message, :allow_nil => true
  validates_length_of       :name,     :maximum => 100

  STANDARD_ROLE = :admin

  # HACK HACK HACK -- how to do attr_accessible from here?
  # prevents a user from submitting a crafted form that bypasses activation
  # anything else you want your user to change should be added here.
  attr_accessible :login, :email, :name, :password, :password_confirmation, :role, :stream_ids, :from_ldap, :api_key

  field :login, :type => String
  field :email, :type => String
  field :name, :type => String
  field :password, :type => String
  field :role, :type => String
  field :crypted_password, :type => String
  field :salt, :type => String
  field :remember_token, :type => String
  field :remember_token_expires_at
  field :last_version_check, :type => Integer
  field :api_key, :type => String
  field :transports, :type => Hash
  field :from_ldap, :type => Boolean, :default => false

  index :login,          :background => true, :unique => true
  index :remember_token, :background => true, :unique => true, :sparse => true
  index :api_key,          :background => true, :unique => true, :sparse => true

  has_and_belongs_to_many :streams, :inverse_of => :users
  has_and_belongs_to_many :favorite_streams,   :class_name => "Stream", :inverse_of => :favorited_streams
  has_and_belongs_to_many :subscribed_streams, :class_name => "Stream", :inverse_of => :subscribers
  references_many :alerted_streams

  # Authenticates a user by their login name and unencrypted password.  Returns the user or nil.
  #
  # uff.  this is really an authorization, not authentication routine.
  # We really need a Dispatch Chain here or something.
  # This will also let us return a human error message.
  #
  def self.authenticate(login, password)
    authenticator = Authenticator.new(login, password)

    if authenticator.authenticated?
      find_or_create_by_credentials(authenticator.credentials)
    end
  end

  def self.find_by_id(_id)
    find(:first, :conditions => {:_id => BSON::ObjectId(_id)})
  end

  def self.find_by_remember_token(token)
    find(:first, :conditions => {:remember_token => token})
  end

  def self.find_by_login(login)
    find(:first, :conditions => {:login => login})
  end
  
  def self.find_by_key(key)
    find(:first, :conditions => {:api_key => key})
  end

  def self.find_or_create_by_credentials(credentials)
    user   = find_by_login(credentials.login)
    params = credentials.to_hash.reverse_merge({ :password              => 'Not needed for auth strategy.',
                                                 :password_confirmation => 'Not needed for auth strategy.',
                                                 :role                  => User::STANDARD_ROLE })

    user || create(params)
  end

  def login=(value)
    write_attribute :login, (value ? value.downcase : nil)
  end

  def display_name
    self.name.blank? ? self.login : self.name
  end

  def email=(value)
    write_attribute :email, (value ? value.downcase : nil)
  end
  
  def generate_api_key
    key = Digest::SHA1.hexdigest(self.login + rand(1000000).to_s + ":" + Time.now.to_s)  
    write_attribute :api_key, key
    return key
  end

  def get_transport_value(typeclass)
    transports.select { |x| x["typeclass"] == typeclass }.first["value"]
  rescue
    nil
  end

  def admin?
    role == "admin"
  end

  def reader?
    role == "reader"
  end

  def roles
    role_symbols
  end

  def role_symbols
    [(role.blank? ? STANDARD_ROLE : role.to_sym)]
  end

  def valid_roles
    [:admin, :reader]
  end
end
