### Normal List
```js
import { ListGroup } from 'components/graylog';

<ListGroup>
  <ListGroupItem>Default</ListGroupItem>
</ListGroup>
```

### Variant List
```js
import { ListGroup } from 'components/graylog';
const styles = ['Danger', 'Warning', 'Success', 'Info'];

<ListGroup>
  {styles.map((style) => (
    <ListGroupItem key={`lgi-${style}`} bsStyle={style.toLowerCase()}>{style}</ListGroupItem>
  ))}
</ListGroup>
```

### Ordered List
```js
import { ListGroup } from 'components/graylog';
const styles = ['Danger', 'Warning', 'Success', 'Info'];

<ListGroup componentClass="ol">
  {styles.map((style, i) => (
    <ListGroupItem key={`lgi-${style}`} bsStyle={style.toLowerCase()} listItem>{i + 1}&#41; {style}</ListGroupItem>
  ))}
</ListGroup>
```

### Disabled List
```js
import { ListGroup } from 'components/graylog';
const styles = ['Danger', 'Warning', 'Success', 'Info'];

<ListGroup>
  {styles.map((style) => (
    <ListGroupItem key={`lgi-${style}`} bsStyle={style.toLowerCase()} disabled>{style}</ListGroupItem>
  ))}
</ListGroup>
```

### Active Items
```js
import { ListGroup } from 'components/graylog';

<ListGroup>
  <ListGroupItem href="#" active>Link 1</ListGroupItem>
  <ListGroupItem href="#">Link 2</ListGroupItem>
  <ListGroupItem href="#" disabled>Link 3</ListGroupItem>
</ListGroup>
```

### Link List
```js
import { ListGroup } from 'components/graylog';
const styles = ['Danger', 'Warning', 'Success', 'Info'];

<ListGroup>
  {styles.map((style) => (
    <ListGroupItem key={`lgi-${style}`} bsStyle={style.toLowerCase()} href="#">{style}</ListGroupItem>
  ))}
</ListGroup>
```
