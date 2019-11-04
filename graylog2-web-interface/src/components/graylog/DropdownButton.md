```js
import { MenuItem } from 'components/graylog';

const styles = ['Primary', 'Danger', 'Warning', 'Success', 'Info', 'Link', 'Default'];

styles.map((style, i) => {
  return (
    <>
      <DropdownButton title={style}
                      bsStyle={style.toLowerCase()}
                      key={`split-button-${i}`}>
        <MenuItem header>Header</MenuItem>
        <MenuItem eventKey="1">Default</MenuItem>
        <MenuItem eventKey="2" active>Active</MenuItem>
        <MenuItem eventKey="4" disabled>Disabled</MenuItem>
        <MenuItem divider />
        <MenuItem eventKey="5">Separated</MenuItem>
      </DropdownButton>{' '}
    </>
  )
})
```
