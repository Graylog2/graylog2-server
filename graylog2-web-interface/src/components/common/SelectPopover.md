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
        <div className="pull-right">
          <SelectPopover id="example-popover" 
                         title="Filter by colour"
                         triggerNode={<Button bsStyle="info" bsSize="small">Select colour</Button>} 
                         items={items}
                         selectedItem={selectedItem}
                         onItemSelect={this.handleItemSelect} />
        </div>
                       
        <p>{selectedItem ? `You have selected ${selectedItem}` : 'Please select a colour!'}</p>
      </div>
    );
  }
});

<SelectPopoverExample />
```
