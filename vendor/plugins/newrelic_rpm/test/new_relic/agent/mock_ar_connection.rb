module ActiveRecord
  class Base
    class << self
      def test_connection(config)
        @connect ||= Connection.new
      end
    end
  end
  
  class Connection
    attr_accessor :throw
    attr_reader :disconnected
    
    def initialize
      @disconnected = false
      @throw = false
    end
    
    def disconnect!()
      @disconnected = true
    end
    
    def find()
      # used to test that we've instrumented this...
    end

    def select_rows(s)
      execute(s)
    end
    def execute(s)
      fail "" if @throw
      if s != "EXPLAIN #{::SQL_STATEMENT}"
        fail "Unexpected sql statement #{s}"        
      end
      s
    end
  end
end


