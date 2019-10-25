```js
import createReactClass from 'create-react-class';
import Immutable from 'immutable';
import { Button } from 'components/graylog';

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

  bulkActionsFactory(selectedNumbers) {
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
                 bulkActionsFactory={this.bulkActionsFactory}
                 itemActionsFactory={this.itemActionsFactory} />
    );
  },
});

<TableListExample />
```

```js
import createReactClass from 'create-react-class';
import Immutable from 'immutable';
import { Button } from 'components/graylog';

const TableListExampleNoBulkActions = createReactClass({
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
                 enableFilter={false}
                 enableBulkActions={false}
                 itemActionsFactory={this.itemActionsFactory} />
    );
  },
});

<TableListExampleNoBulkActions />
```
