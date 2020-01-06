```js
import { MenuItem } from 'components/graylog';

const styles = ['Primary', 'Danger', 'Warning', 'Success', 'Info', 'Link', 'Default'];

styles.map((style, i) => {
  return (
    <p key={`button-example-${style}-${i}`}>
      <Button bsStyle={style.toLowerCase()}>{style}</Button>{' '}
      <Button active bsStyle={style.toLowerCase()}>{style} Active</Button>{' '}
      <Button disabled bsStyle={style.toLowerCase()}>{style} Disabled</Button>{' '}
    </p>
  )
})
```
