import * as React from 'react';

import Menu from 'components/bootstrap/Menu';

const wrapWithMenu = <Props, >(Component: React.ComponentType<Props>): React.ComponentType<Props> => (props: Props) => (
  <Menu opened>
    <Menu.Dropdown>
      <Component {...props} />
    </Menu.Dropdown>
  </Menu>
);

export default wrapWithMenu;
