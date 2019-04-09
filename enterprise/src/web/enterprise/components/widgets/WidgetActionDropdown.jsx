// @flow strict
import * as React from 'react';

import ActionDropdown from 'enterprise/components/common/ActionDropdown';

import styles from './Widget.css';

type Props = {
  children: React.Node,
};

const WidgetActionDropdown = ({ children }: Props) => {
  const widgetActionDropdownCaret = <i className={`fa fa-chevron-down ${styles.widgetActionDropdownCaret} ${styles.tonedDown}`} />;
  return (
    <ActionDropdown element={widgetActionDropdownCaret}>
      {children}
    </ActionDropdown>
  );
};

export default WidgetActionDropdown;
