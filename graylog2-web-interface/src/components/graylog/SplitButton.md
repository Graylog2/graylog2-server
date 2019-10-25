```js
import { MenuItem } from 'components/graylog';

const styles = ['Primary', 'Danger', 'Warning', 'Success', 'Info', 'Link', 'Default'];

styles.map((style, i) => {
  return (
    <>
      <SplitButton title={style} bsStyle={style.toLowerCase()} key={`split-button-${i}`}>
        <MenuItem eventKey="1">Action</MenuItem>
        <MenuItem eventKey="2">Another action</MenuItem>
        <MenuItem eventKey="3">Something else here</MenuItem>
        <MenuItem divider />
        <MenuItem eventKey="4">Separated link</MenuItem>
      </SplitButton>{' '}
    </>
  )
})
```
