if defined? Authlogic::Session::Base
  Authlogic::Session::Base.class_eval do
  #  add_method_tracer :record, 'Custom/Authlogic/record'
    class << self
      add_method_tracer :find, 'Custom/Authlogic/find'
    end
  end
end
