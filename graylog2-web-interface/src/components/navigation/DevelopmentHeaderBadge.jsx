// @flow strict
import React from 'react';
import PropTypes from 'prop-types';

import AppConfig from 'util/AppConfig';
import { Badge } from 'components/graylog';

type Props = {
  smallScreen: boolean,
};

const DevelopmentHeaderBadge = ({ smallScreen }: Props) => {
  const smallScreenClass = smallScreen ? 'small-scrn-badge' : '';
  return AppConfig.gl2DevMode() ? <Badge className={`${smallScreenClass} dev-badge`} bsStyle="danger">DEV</Badge> : null;
};

DevelopmentHeaderBadge.propTypes = {
  smallScreen: PropTypes.bool,
};

DevelopmentHeaderBadge.defaultProps = {
  smallScreen: false,
};

export default DevelopmentHeaderBadge;
