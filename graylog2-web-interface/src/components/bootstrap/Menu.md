#### Simple implementation
```tsx
import { Menu, MenuItem, DeleteMenuItem, Button } from 'components/bootstrap';

<Menu>
  <Menu.Target>
    <Button>Open menu</Button>
  </Menu.Target>
  <Menu.Dropdown>
    <MenuItem onClick={() => console.log('on click menu item')}>Edit</MenuItem>
    <Menu.Divider />
    <DeleteMenuItem onClick={() => console.log('on click menu item')} />
  </Menu.Dropdown>
</Menu>
```
