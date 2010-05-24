# This is used to allow obfuscators to be chained.  

class NewRelic::ChainedCall
  def initialize(block1, block2)
    @block1 = block1
    @block2 = block2
  end
  
  def call(sql)
    sql = @block1.call(sql)
    @block2.call(sql)
  end
end