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
      selectedItem: undefined,
    };
  },
  
  handleItemSelect(item) {
    this.setState({ selectedItem: item });
  },
  
  render() {
    const selectedItem = this.state.selectedItem;

    return (
      <div>
        <div style={{ display: 'inline-block', marginRight: 20 }}>
          <SelectPopover id="example-popover" 
                         title="Filter by color"
                         triggerNode={<Button bsStyle="info" bsSize="small">Select color</Button>}
                         items={items}
                         selectedItem={selectedItem}
                         onItemSelect={this.handleItemSelect}
                         displayDataFilter={false} />
        </div>
        
        {selectedItem ? `You have selected ${selectedItem}` : 'Please select a color!'}
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
      selectedItem: undefined,
    };
  },
  
  handleItemSelect(item) {
    this.setState({ selectedItem: item });
  },
  
  formatItem(item) {
    return (
      <span>
        <i className="fa fa-fw fa-square" style={{ color: item }} /> {item}
      </span>
    )
  },
  
  render() {
    const selectedItem = this.state.selectedItem;

    return (
      <div>
        <div style={{ display: 'inline-block', marginRight: 20 }}>
          <SelectPopover id="example-popover-formatted" 
                         title="Filter by color"
                         triggerNode={<Button bsStyle="info" bsSize="small">Select color</Button>}
                         items={items}
                         itemFormatter={this.formatItem}
                         selectedItem={selectedItem}
                         onItemSelect={this.handleItemSelect}
                         filterPlaceholder="Filter by color" />
        </div>
        
        {selectedItem ? `You have selected ${selectedItem}` : 'Please select another color!'}
      </div>
    );
  }
});

<SelectPopoverFormattedExample />
```
