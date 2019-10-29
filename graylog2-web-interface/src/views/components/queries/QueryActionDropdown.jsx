// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import { Icon } from 'components/common';
import ActionDropdown from 'views/components/common/ActionDropdown';

type Props = {
  children: React.Node,
};

const QueryActionDropdown = ({ children }: Props) => (
  <ActionDropdown element={<Icon name="chevron-down" data-testid="query-action-dropdown" />}>
    {children}
  </ActionDropdown>
);

QueryActionDropdown.propTypes = {
  children: PropTypes.node.isRequired,
};

export default QueryActionDropdown;
