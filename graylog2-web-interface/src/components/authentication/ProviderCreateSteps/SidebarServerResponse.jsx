// @flow strict
import * as React from 'react';
import PropTypes from 'prop-types';

import { Collapse } from 'components/graylog';

type Props = {
  children: string,
};

const SidebarConnectionCheck = ({ children }: Props) => (
  <>
    <Collapse in={false} timeout={0}>
      <div>{children}</div>
    </Collapse>

  </>
);

SidebarConnectionCheck.propTypes = {
  children: PropTypes.string,
};

SidebarConnectionCheck.defaultProps = {
  children: 'Hello World!',
};

export default SidebarConnectionCheck;
