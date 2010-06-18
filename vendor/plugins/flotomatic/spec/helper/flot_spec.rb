require File.join(File.dirname(__FILE__), '..', 'spec_helper')

describe FlotHelper do  
  include FlotHelper
  
  before(:each) do
    @class       = "stylin"
    @placeholder = "placeholder"
    @flot        = Flot.new(@placeholder, :class => @class)
    @data        = "data"
    @options     = "options"

    @flot.data.stub!(:to_json).and_return @data
    @flot.options.stub!(:to_json).and_return @options
  end
  
  describe "flot_includes" do
    it "should have a script tag" do
      flot_includes.should have_tag('script')
    end

    it "should include jquery" do
      flot_includes.should match(/jquery\.min\.js/)
    end

    it "should include jquery.flot" do
      flot_includes.should match(/jquery\.flot/)
    end

    it "should include excanvas" do
      flot_includes.should match(/excanvas/)
    end

    it "should include selection when :include => :selection" do
      flot_includes(:include => :selection).should match(/jquery\.flot\.selection\.min/)
    end
  end
  
  describe "flot_canvas" do
    it "should create a div with id of placeholder" do
      flot_canvas("placeholder").should have_tag('div#placeholder')
    end

    it "should create pass through html options" do
      flot_canvas("placeholder", :class => 'stylin').should have_tag('div[id=?][class=?]', 'placeholder', 'stylin')
    end
  
    it "should work similiarly for with a object" do
      flot = Flot.new("placeholder", :class => 'stylin')
      flot_canvas(flot).should have_tag('div[id=?][class=?]', flot.placeholder, 'stylin')
    end
    
    it "should also overide the object with a passed in class" do
      flot = Flot.new("placeholder", :class => 'stylin')
      flot_canvas(flot, :class => 'profilin').should have_tag('div[id=?][class=?]', flot.placeholder, 'profilin')
    end
  end

  describe "flot_graph" do
    describe "ready function" do
      it "should be a javascript script" do
        flot_graph(@placeholder, @flot).should have_tag('script[type=?]', 'text/javascript')
        flot_graph(@placeholder, @flot).should match(/\}\s*\)\s*;/)
      end

      it "should generate generate ready function (no conflict with prototype)" do
        flot_graph(@placeholder, @flot).should match(/jQuery\(function\(\)\s*\{/)
      end
    end

    describe "javascript variables" do
      it "should set data" do
        flot_graph(@placeholder, @flot).should =~ /var\s+data\s*=\s*#{@data}\s*;/
      end

      it "should set options" do
        flot_graph(@placeholder, @flot).should =~ /var\s+options\s*=\s*#{@options}\s*;/
      end

      it "should set flotomatic" do
        flot_graph(@placeholder, @flot).should =~ /var\s+flotomatic\s*=\s*new\s+Flotomatic\('#{@placeholder}',\s*data\s*,\s*options\s*\)\s*;/
      end
    end
    
    describe "evaluating a blog" do
      it "should pass it through to the end of the javascript" do
        _erbout = ''
        flot_graph(@placeholder, @flot) { _erbout.concat "// My favorite number is #{3 + 4}" }.should =~ /My favorite number is 7/
      end

      it "should output the plot javascript function with appropriate arguments" do
        flot_graph(@placeholder, @flot) { flot_plot }.should =~  /flotomatic\.graph\(\)/
      end
    end
  end
end
