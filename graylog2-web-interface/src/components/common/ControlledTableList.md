```js
import createReactClass from 'create-react-class';
import Immutable from 'immutable';
import { Col, Row } from 'components/graylog';

const ControlledTableListExample = createReactClass({
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

  formatItems(items) {
    return items.map(item => {
      return (
        <ControlledTableList.Item key={item.id}>
          <Row className="row-sm">
            <Col md={12}>
              <h5>{item.title} <small>{item.description}</small></h5>
            </Col>
          </Row>
          <Row className="row-sm">
            <Col md={12}>
              #{item.id}
            </Col>
          </Row>
        </ControlledTableList.Item>
      )
    });
  },

  render() {
    const { items } = this.state;

    return (
      <ControlledTableList>
        <ControlledTableList.Header>
          Numbers
        </ControlledTableList.Header>
        {this.formatItems(items)}
      </ControlledTableList>
    );
  },
});

<ControlledTableListExample />
```
