```js
import createReactClass from 'create-react-class';
import { Button } from 'components/graylog';

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
import createReactClass from 'create-react-class';
import { Button, Badge } from 'components/graylog';
import { Icon } from 'components/common';

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
        <Icon name="square" fixedWidth style={{ color: item }} /> {item}
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
