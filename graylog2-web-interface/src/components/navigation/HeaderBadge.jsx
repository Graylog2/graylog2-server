// @flow strict
import React from 'react';
import PropTypes from 'prop-types';

import AppConfig from 'util/AppConfig';
import { Badge } from 'components/graylog';

type Props = {
  smallScreen: boolean,
};

const HeaderBadge = ({ smallScreen }: Props) => {
  const smallScreenClass = smallScreen ? 'small-scrn-badge' : '';
  return AppConfig.gl2DevMode() ? <Badge className={`${smallScreenClass} dev-badge`} bsStyle="danger">DEV</Badge> : null;
};

HeaderBadge.propTypes = {
  smallScreen: PropTypes.bool,
};

HeaderBadge.defaultProps = {
  smallScreen: false,
};

export default HeaderBadge;
