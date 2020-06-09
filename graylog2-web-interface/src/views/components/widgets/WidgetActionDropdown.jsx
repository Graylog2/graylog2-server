// @flow strict
import * as React from 'react';

import ActionDropdown from 'views/components/common/ActionDropdown';
import { IconButton } from 'components/common';


type Props = {
  children: React.Node,
};

const WidgetActionDropdown = ({ children }: Props) => {
  const widgetActionDropdownCaret = <IconButton data-testid="widgetActionDropDown" name="chevron-down" title="Open actions dropdown" />;
  return (
    <ActionDropdown element={widgetActionDropdownCaret}>
      {children}
    </ActionDropdown>
  );
};

export default WidgetActionDropdown;
