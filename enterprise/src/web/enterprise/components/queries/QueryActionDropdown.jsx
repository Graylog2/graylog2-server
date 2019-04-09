// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import ActionDropdown from 'enterprise/components/common/ActionDropdown';

type Props = {
  children: React.Node,
};

const QueryActionDropdown = ({ children }: Props) => (
  <ActionDropdown element={<i className="fa fa-chevron-down" />}>
    {children}
  </ActionDropdown>
);

QueryActionDropdown.propTypes = {
  children: PropTypes.node.isRequired,
};

export default QueryActionDropdown;
