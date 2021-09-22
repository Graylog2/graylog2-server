```js
import { Nav, NavDropdown, MenuItem } from 'components/graylog';

<Nav activeKey="1.1">
  <NavDropdown eventKey="1" title="Dropdown" id="nav-dropdown">
    <MenuItem eventKey="1.1">Action</MenuItem>
    <MenuItem eventKey="1.2">Another action</MenuItem>
    <MenuItem eventKey="1.3">Something else here</MenuItem>
    <MenuItem divider />
    <MenuItem eventKey="1.4">Separated link</MenuItem>
  </NavDropdown>
</Nav>
```
