```js
import { Nav, MenuItem } from './index';

<Nav>
  <NavDropdown eventKey="1" title="Dropdown" id="nav-dropdown">
    <MenuItem header>Header</MenuItem>
    <MenuItem eventKey="1.1">Default</MenuItem>
    <MenuItem eventKey="1.2" active>Active</MenuItem>
    <MenuItem eventKey="1.3" disabled>Disabled</MenuItem>
    <MenuItem divider />
    <MenuItem eventKey="1.4">Separated</MenuItem>
  </NavDropdown>
</Nav>
```
