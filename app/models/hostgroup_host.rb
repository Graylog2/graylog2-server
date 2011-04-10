class HostgroupHost
  include Mongoid::Document

  validates_presence_of :ruletype, :hostname

  referenced_in :hostgroup

  field :ruletype, :type => Integer
  field :hostname, :type => String

  TYPE_SIMPLE = 0
  TYPE_REGEX = 1

  def simple?
    ruletype == TYPE_SIMPLE
  end

  def regex?
    ruletype == TYPE_REGEX
  end

  def to_condition
    if simple?
      hostname
    elsif regex?
      /#{hostname}/
    end
  end
end
