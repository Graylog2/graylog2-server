```js
import { MenuItem } from 'components/graylog';

const styles = ['Primary', 'Danger', 'Warning', 'Success', 'Info', 'Link', 'Default'];

styles.map((style) => {
  return (
    <span key={`split-button-${style.toLowerCase()}`}>
      <SplitButton title={style}
                   bsStyle={style.toLowerCase()}
                   id={`split-button-${style.toLowerCase()}`}>
        <MenuItem header>Header</MenuItem>
        <MenuItem eventKey="1">Default</MenuItem>
        <MenuItem eventKey="2" active>Active</MenuItem>
        <MenuItem eventKey="4" disabled>Disabled</MenuItem>
        <MenuItem divider />
        <MenuItem eventKey="5">Separated</MenuItem>
      </SplitButton>{' '}
    </span>
  )
})
```
