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
                         title="Filter by colour"
                         triggerNode={<Button bsStyle="info" bsSize="small">Select colour</Button>} 
                         items={items}
                         selectedItem={selectedItem}
                         onItemSelect={this.handleItemSelect}
                         displayDataFilter={false} />
        </div>
        
        {selectedItem ? `You have selected ${selectedItem}` : 'Please select a colour!'}
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
  'Black',
  'Blue',
  'Green',
  'Red',
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
        <Badge style={{ backgroundColor: item }}>&nbsp;</Badge> {item}
      </span>
    )
  },
  
  render() {
    const selectedItem = this.state.selectedItem;

    return (
      <div>
        <div style={{ display: 'inline-block', marginRight: 20 }}>
          <SelectPopover id="example-popover-formatted" 
                         title="Filter by colour"
                         triggerNode={<Button bsStyle="info" bsSize="small">Select colour</Button>} 
                         items={items}
                         itemFormatter={this.formatItem}
                         selectedItem={selectedItem}
                         onItemSelect={this.handleItemSelect}
                         filterPlaceholder="Filter by colour" />
        </div>
        
        {selectedItem ? `You have selected ${selectedItem}` : 'Please select a colour!'}
      </div>
    );
  }
});

<SelectPopoverFormattedExample />
```
