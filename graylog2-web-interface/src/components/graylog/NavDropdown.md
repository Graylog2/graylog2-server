```js
import { Nav, NavDropdown, MenuItem } from 'components/graylog';

<Nav activeKey="4.1">
  <NavDropdown eventKey="4" title="Dropdown" id="nav-dropdown">
    <MenuItem eventKey="4.1">Action</MenuItem>
    <MenuItem eventKey="4.2">Another action</MenuItem>
    <MenuItem eventKey="4.3">Something else here</MenuItem>
    <MenuItem divider />
    <MenuItem eventKey="4.4">Separated link</MenuItem>
  </NavDropdown>
</Nav>
```
