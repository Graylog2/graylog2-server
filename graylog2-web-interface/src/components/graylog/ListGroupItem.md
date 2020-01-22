### Normal List
```js
import { ListGroup } from 'components/graylog';

<ListGroup>
  <ListGroupItem>Item 1</ListGroupItem>
  <ListGroupItem>Item 2</ListGroupItem>
  <ListGroupItem>Item 3</ListGroupItem>
</ListGroup>
```

### Linked Items
```js
import { ListGroup } from 'components/graylog';

<ListGroup>
  <ListGroupItem href="#">Link 1</ListGroupItem>
  <ListGroupItem href="#">Link 2</ListGroupItem>
  <ListGroupItem href="#">Link 3</ListGroupItem>
</ListGroup>
```

### Stateful Items
```js
import { ListGroup } from 'components/graylog';

<ListGroup>
  <ListGroupItem href="#" active>Link 1</ListGroupItem>
  <ListGroupItem href="#">Link 2</ListGroupItem>
  <ListGroupItem href="#" disabled>Link 3</ListGroupItem>
</ListGroup>
```

### Variant Items
```js
import { ListGroup } from 'components/graylog';
const styles = ['Danger', 'Warning', 'Success', 'Info'];

<ListGroup componentClass="ol">
  {styles.map((style, i) => (
    <ListGroupItem key={`lgi-${style}`} bsStyle={style.toLowerCase()} listItem>{i + 1}&#41; {style}</ListGroupItem>
  ))}
</ListGroup>
```

### w/ Headers
```js
import { ListGroup } from 'components/graylog';

<ListGroup>
  <ListGroupItem header="Heading 1">Some body text</ListGroupItem>
  <ListGroupItem header="Heading 2" href="#">
    Linked item
  </ListGroupItem>
  <ListGroupItem header="Heading 3" bsStyle="danger">
    Danger styling
  </ListGroupItem>
</ListGroup>
```
