### Default

```js
import {Nav, NavItem, NavDropdown, MenuItem} from 'components/bootstrap';

<Navbar>
  <Nav activeKey={1}>
    <NavItem eventKey={1} href="/home">
      NavItem 1 content
    </NavItem>
    <NavItem eventKey={2} title="Item">
      NavItem 2 content
    </NavItem>
    <NavItem eventKey={3} disabled>
      NavItem 3 disabled
    </NavItem>
    <NavDropdown eventKey="4" title="Dropdown" id="nav-dropdown">
      <MenuItem eventKey="4.1">Action</MenuItem>
      <MenuItem eventKey="4.2">Another action</MenuItem>
      <MenuItem eventKey="4.3">Something else here</MenuItem>
      <MenuItem divider />
      <MenuItem eventKey="4.4">Separated link</MenuItem>
    </NavDropdown>
  </Nav>
</Navbar>
```

### Inversed

```js
import {Nav, NavItem, NavDropdown, MenuItem} from 'components/bootstrap';

<Navbar inverse>
  <Nav activeKey={1}>
    <NavItem eventKey={1} href="/home">
      NavItem 1 content
    </NavItem>
    <NavItem eventKey={2} title="Item">
      NavItem 2 content
    </NavItem>
    <NavItem eventKey={3} disabled>
      NavItem 3 disabled
    </NavItem>
    <NavDropdown eventKey="4" title="Dropdown" id="nav-dropdown">
      <MenuItem eventKey="4.1">Action</MenuItem>
      <MenuItem eventKey="4.2">Another action</MenuItem>
      <MenuItem eventKey="4.3">Something else here</MenuItem>
      <MenuItem divider />
      <MenuItem eventKey="4.4">Separated link</MenuItem>
    </NavDropdown>
  </Nav>
</Navbar>
```
