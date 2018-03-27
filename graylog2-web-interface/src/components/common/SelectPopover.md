```js
const createReactClass = require('create-react-class');
const Button = require('react-bootstrap').Button;

const items = [
  'Black',
  'Blue',
  'Green',
  'Red',
  'White',
  'Yellow',
];

const SelectPopoverExample = createReactClass({
  getInitialState() {
    return {
      selectedColor: undefined,
    };
  },
  
  handleItemSelect(item) {
    this.setState({ selectedColor: item[0] });
  },
  
  render() {
    const selectedColor = this.state.selectedColor;

    return (
      <div>
        <div style={{ display: 'inline-block', marginRight: 20 }}>
          <SelectPopover id="example-popover" 
                         title="Filter by color"
                         triggerNode={<Button bsStyle="info" bsSize="small">Select color</Button>}
                         items={items}
                         selectedItems={selectedColor ? [selectedColor] : []}
                         onItemSelect={this.handleItemSelect}
                         displayDataFilter={false}
                         clearSelectionText="Clear color selection"/>
        </div>
        
        {selectedColor ? `You have selected ${selectedColor}` : 'Please select a color!'}
      </div>
    );
  }
});

<SelectPopoverExample />
```

```js
const createReactClass = require('create-react-class');
const Badge = require('react-bootstrap').Badge;
const Button = require('react-bootstrap').Button;

const items = [
  'AliceBlue',
  'Aqua',
  'Black',
  'Blue',
  'Brown',
  'Cyan',
  'DarkMagenta',
  'Gold',
  'Green',
  'Magenta',
  'Navy',
  'Red',
  'SeaGreen',
  'Turquoise',
  'White',
  'Yellow',
];

const SelectPopoverFormattedExample = createReactClass({
  getInitialState() {
    return {
      selectedColors: [],
    };
  },
  
  handleItemSelect(item) {
    this.setState({ selectedColors: item });
  },
  
  formatItem(item) {
    return (
      <span>
        <i className="fa fa-fw fa-square" style={{ color: item }} /> {item}
      </span>
    )
  },
  
  render() {
    const selectedColors = this.state.selectedColors;

    return (
      <div>
        <div style={{ display: 'inline-block', marginRight: 20 }}>
          <SelectPopover id="example-popover-formatted" 
                         title="Filter by color"
                         triggerNode={<Button bsStyle="info" bsSize="small">Select color</Button>}
                         items={items}
                         itemFormatter={this.formatItem}
                         onItemSelect={this.handleItemSelect}
                         filterPlaceholder="Filter by color"
                         multiple={true}
                         selectedItems={selectedColors} />
        </div>
        
        {selectedColors.length > 0 ? `You have selected ${selectedColors.join(', ')}` : 'Please select some colors!'}
      </div>
    );
  }
});

<SelectPopoverFormattedExample />
```
