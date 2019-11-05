// @flow strict
import * as React from 'react';

import ActionDropdown from 'views/components/common/ActionDropdown';
import { Icon } from 'components/common';

import styles from './Widget.css';

type Props = {
  children: React.Node,
};

const WidgetActionDropdown = ({ children }: Props) => {
  const widgetActionDropdownCaret = <Icon data-testid="widgetActionDropDown" name="chevron-down" className={`${styles.widgetActionDropdownCaret} ${styles.tonedDown}`} />;
  return (
    <ActionDropdown element={widgetActionDropdownCaret}>
      {children}
    </ActionDropdown>
  );
};

export default WidgetActionDropdown;
