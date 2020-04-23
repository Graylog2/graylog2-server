// @flow strict
import React from 'react';
import PropTypes from 'prop-types';

import AppConfig from 'util/AppConfig';
import { Badge } from 'components/graylog';

type Props = {
  smallScreen: boolean,
}

const HeaderBadge = ({ smallScreen = false }: Props) => {
  const smallScreenClass = smallScreen ? 'small-scrn-badge' : '';
  const devBadge = AppConfig.gl2DevMode() ? <Badge className={`${smallScreenClass} dev-badge`} bsStyle="danger">DEV</Badge> : null;
  return (
    <>
      {devBadge}
    </>
  );
};

HeaderBadge.propTypes = {
  smallScreen: PropTypes.bool,
};

HeaderBadge.defaultProps = {
  smallScreen: false,
};

export default HeaderBadge;
