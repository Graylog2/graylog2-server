#### Simple implementation
```tsx
import { Menu, MenuItem, MenuItemDelete, Button } from 'components/bootstrap';

<Menu>
  <Menu.Target>
    <Button>Open menu</Button>
  </Menu.Target>
  <Menu.Dropdown>
    <MenuItem onClick={() => console.log('on click menu item')}>Edit</MenuItem>
    <Menu.Divider />
    <MenuItemDelete onClick={() => console.log('on click menu item')}>Delete</MenuItemDelete>
  </Menu.Dropdown>
</Menu>
```
