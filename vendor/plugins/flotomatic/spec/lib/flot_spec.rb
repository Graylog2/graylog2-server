require File.join(File.dirname(__FILE__), '..', '..', 'lib', 'flot')

Record = Struct.new(:frequency, :amplitude)

describe "Flot" do
  before(:each) do
    @collection   = [Record.new(1,15), Record.new(2,30), Record.new(4,40)]
    @data         = [          [1,15],           [2,30],           [4,40]]

    @placeholder  = "placeholder"
    @html_options = {:class => "stylin"}
    @options      = {:bars => {:show => true}}

    @flot         = Flot.new(@placeholder, @html_options) {|flot| flot.options = @options }
  end
  
  describe "initialization" do
    it "should take an optional 'placeholder' argument" do
      Flot.new(@placeholder).placeholder.should == @placeholder
      Flot.new.placeholder.should == nil
    end

    it "should take an optional html_options argument (defaulting to 300px height)" do
      Flot.new(@html_options).placeholder.should == @html_options
      Flot.new.html_options.should == {:style => "height: 300px"}
    end
    
    it "should set data and options to empty by default" do
      Flot.new.data.should == []
      Flot.new.options.should == {}
    end
    
    it "should take a block setting attributes" do
      flot = Flot.new {|f| f.data = @data ; f.options = @options }
      flot.data.should == @data
      flot.options.should == @options
    end
    
    it "should default lines, bars, points, and legend to display, when called" do
      flot = Flot.new(@placeholder, @html_options) {|flot|  flot.lines ; flot.bars ; flot.points ; flot.legend }
      [:lines, :points, :bars, :legend].each {|opt| flot.options[opt].should == {:show => true}}
    end

    it "should set lines, bars, points, and legend to argument if provided" do
      [:lines, :points, :legend].each {|method| @flot.send(method, {:show => false})}
      [:lines, :points, :legend].each {|opt| @flot.options[opt].should == {:show => false}}

      @flot.bars(:barWidth => 100)
      @flot.options[:bars].should == {:barWidth => 100}
    end
    
    it "should pass all other methods along to the options hash" do
      @flot.stuff(:value => 1)
      @flot.options[:stuff].should == {:value => 1}
    end
  end
  
  describe "series" do
    it "should add label, data, and options as a hash in the data array" do
      @flot.series("label1", @data, {:lines => {:show => true}})
      @flot.series("label2", [[2,2]], {:points => {:show => true}})
      @flot.data.first.should == {:label => "label1", :data => @data, :lines => {:show => true}}
      @flot.data.last.should == {:label => "label2", :data => [[2,2]], :points => {:show => true}}
    end

    it "should use the class's options if none are provided" do
      @flot.series("label1", @data)
      @flot.series("label2", [[2,2]])
      @flot.data.first.should == {:label => "label1", :data => @data, :bars => {:show => true}}
      @flot.data.last.should == {:label => "label2", :data => [[2,2]], :bars => {:show => true}}
    end
  end
  
  describe "series_for" do
    it "should create a series from a collection" do
      @flot.should_receive(:series).with('label', @data, {})
      @flot.series_for 'label', @collection, :x => :frequency, :y => :amplitude
    end

    it "should pass options through" do
      @flot.should_receive(:series).with('label', @data, {:lines => {:show => true}})
      @flot.series_for 'label', @collection, :x => :frequency, :y => :amplitude, :options => {:lines => {:show => true}}
    end

    it "should accept procs to retrieve & transform data" do
      @flot.should_receive(:series).with('These go to 11', @data.map {|v| [v[0], v[1] * 11]}, {})
      @flot.series_for 'These go to 11', @collection, :x => :frequency, :y => lambda {|record| record.amplitude * 11}
    end
    
    it "should user filter block to filter collection" do
      @flot.series_for 'label1', @collection, :x => :frequency, :y => :amplitude, :options => {:lines => {:show => true}}
      @flot.filter {|collection| collection.select {|r| r.frequency <  4}}
      @flot.series_for 'label2', @collection, :x => :frequency, :y => :amplitude, :options => {:lines => {:show => true}}
      @flot.data.first[:data].any? {|pair| pair.first >= 4}.should be_true
      @flot.data.last[:data].any? {|pair| pair.first >= 4}.should be_false
    end
  end
end















