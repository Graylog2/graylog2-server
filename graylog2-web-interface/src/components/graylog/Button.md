```js
import { MenuItem } from 'components/graylog';

const styles = ['Primary', 'Danger', 'Warning', 'Success', 'Info', 'Link', 'Default'];

styles.map((style, i) => {
  return (
    <p>
      <Button bsStyle={style.toLowerCase()} key={`button-${style}-${i}`}>{style}</Button>{' '}
      <Button active bsStyle={style.toLowerCase()} key={`button-active-${style}-${i}`}>{style} Active</Button>{' '}
      <Button disabled bsStyle={style.toLowerCase()} key={`button-disabled-${style}-${i}`}>{style} Disabled</Button>{' '}
    </p>
  )
})
```
