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
