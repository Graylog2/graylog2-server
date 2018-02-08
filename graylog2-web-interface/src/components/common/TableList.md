```js
const createReactClass = require('create-react-class');
const Immutable = require('immutable');
const Button = require('react-bootstrap').Button;

const TableListExample = createReactClass({
  getInitialState() {
    return {
      items: Immutable.List([
        { id: '1', title: 'One', secret_key: 'uno', description: 'First number' },
        { id: '2', title: 'Two', secret_key: 'dos', description: 'Second number' },
        { id: '3', title: 'Three', secret_key: 'tres', description: 'Third number' },
        { id: '4', title: 'Four', secret_key: 'cuatro', description: 'Fourth number' },
        { id: '5', title: 'Five', secret_key: 'cinco', description: 'Fifth number' },
      ]),
    };
  },

  action(text) {
    return () => {
      alert(`This is an action for "${text}"`);
    };
  },

  headerActionsFactory(selectedNumbers) {
    return (
      <Button bsStyle="info"
              bsSize="xsmall"
              onClick={this.action(selectedNumbers.join(', '))}>
        Bulk-operation
      </Button>
    );
  },

  itemActionsFactory(selectedNumber) {
    return (
      <Button bsStyle="primary"
              bsSize="xsmall"
              onClick={this.action(JSON.stringify(selectedNumber))}>
        Do something
      </Button>
    );
  },

  render() {
    const { items } = this.state;

    return (
      <TableList items={items}
                 filterKeys={['title', 'secret_key']}
                 filterLabel=""
                 headerActionsFactory={this.headerActionsFactory}
                 itemActionsFactory={this.itemActionsFactory} />
    );
  },
});

<TableListExample />
```

```js
const createReactClass = require('create-react-class');
const Immutable = require('immutable');
const Button = require('react-bootstrap').Button;
const DropdownButton = require('react-bootstrap').DropdownButton;
const MenuItem = require('react-bootstrap').MenuItem;

const items = Immutable.List([
  { id: '1', title: 'One', secret_key: 'uno', description: 'First number' },
  { id: '2', title: 'Two', secret_key: 'dos', description: 'Second number' },
  { id: '3', title: 'Three', secret_key: 'tres', description: 'Third number' },
  { id: '4', title: 'Four', secret_key: 'cuatro', description: 'Fourth number' },
  { id: '5', title: 'Five', secret_key: 'cinco', description: 'Fifth number' },
]);

const TableListExampleExternalFilter = createReactClass({
  getInitialState() {
    return {
      items: items,
      filterKey: undefined,
    };
  },

  action(text) {
    return () => {
      alert(`This is an action for "${text}"`);
    };
  },

  toggleFilterLess(filterKey) {
    const nextState = {};
    if (this.state.filterKey === filterKey) {
      nextState.items = items;
      nextState.filterKey = undefined;
    } else {
      nextState.items = items.filter(i => Number(i.id) < 3);
      nextState.filterKey = filterKey;
    }
    this.setState(nextState);
  },

  filterGreater(filterKey) {
    const nextState = {};
    if (this.state.filterKey === filterKey) {
      nextState.items = items;
      nextState.filterKey = undefined;
    } else {
      nextState.items = items.filter(i => Number(i.id) > 3);
      nextState.filterKey = filterKey;
    }
    this.setState(nextState);
  },

  headerFiltersFactory() {
    return (
      <DropdownButton id="filters"
                      bsStyle="link"
                      bsSize="xsmall"
                      title="Filters"
                      pullRight>
          <MenuItem eventKey={1} onSelect={this.toggleFilterLess} active={this.state.filterKey === 1}>
            Less than 3
          </MenuItem>
          <MenuItem eventKey={2} onSelect={this.filterGreater} active={this.state.filterKey === 2}>
            Greater than 3
          </MenuItem>
      </DropdownButton>
    );
  },

  headerActionsFactory(selectedNumbers) {
    return (
      <Button bsStyle="info"
              bsSize="xsmall"
              onClick={this.action(selectedNumbers.join(', '))}>
        Bulk-operation
      </Button>
    );
  },

  render() {
    const { items } = this.state;

    return (
      <TableList items={items}
                 filterKeys={['title', 'secret_key']}
                 filterLabel=""
                 headerFiltersFactory={this.headerFiltersFactory}
                 headerActionsFactory={this.headerActionsFactory} />
    );
  },
});

<TableListExampleExternalFilter />
```
