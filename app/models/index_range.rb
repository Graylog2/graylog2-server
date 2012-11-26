class IndexRange
  include Mongoid::Document

   field :index, :type => String
   field :start, :type => Integer

end