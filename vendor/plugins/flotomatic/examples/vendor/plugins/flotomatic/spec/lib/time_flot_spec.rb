require File.join(File.dirname(__FILE__), '..', 'spec_helper')

TimeRec = Struct.new(:day, :count)

describe "TimeFlot" do
  before(:each) do
    @collection = [TimeRec.new(Date.yesterday,15), TimeRec.new(Date.today,30), TimeRec.new(Date.tomorrow,40)]
    @data       = [[TimeFlot.js_time_from(Date.yesterday),15], [TimeFlot.js_time_from(Date.today),30], [TimeFlot.js_time_from(Date.tomorrow),40]]
    @flot       = TimeFlot.new {|flot| flot.bars }
    
    # note>: this doesn't work and it _could_ be a problem
    # @options    = {:bars => {:show => true}}
    # @flot       = TimeFlot.new {|flot| flot.options = @options }
  end
  
  it "should set bars to 1 day wide by default" do
    @flot.bars
    @flot.options[:bars].should == {:show => true, :barWidth => TimeFlot::BAR_WIDTH, :align => "center"}
  end
  
  describe "series_for" do
    it "should default the time axis to the x axis" do
      @flot.series_for("Time Plot", @collection, :x => :day, :y => :count)
      @flot.options[:xaxis][:mode].should == 'time'
      @flot.is_time_axis?(:xaxis).should_not be_false
    end
    
    it "should use convert_to_js_time method to transform the time axis if no lambda is provided" do
      @flot.should_receive(:series).with("Time Plot", @data, {})
      @flot.series_for("Time Plot", @collection, :x => :day, :y => :count)
    end
    
    it "should be able to set the time axis" do
      flot = TimeFlot.new(:yaxis)
      flot.series_for("Time Plot", @collection, :x => :count, :y => :day)
      flot.options[:yaxis][:mode].should == 'time'
      flot.is_time_axis?(:yaxis).should be_true
    end
  end
end