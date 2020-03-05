import React, { useEffect } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import CombinedProvider from 'injection/CombinedProvider';
import AppConfig from 'util/AppConfig';
import { Badge } from 'components/graylog';
import connect from 'stores/connect';

const { ConfigurationsActions, ConfigurationsStore } = CombinedProvider.get('Configurations');

const CUSTOMIZATION_CONFIG = 'org.graylog2.configuration.Customization';

const HeaderBadge = ({ configuration }) => {
  useEffect(() => {
    ConfigurationsActions.list(CUSTOMIZATION_CONFIG);
  }, []);

  const config = configuration[CUSTOMIZATION_CONFIG];
  const badgeEnabled = config && config.badge_enable;

  if (badgeEnabled) {
    const StyledBadge = styled(Badge)`
      background-color: ${config.badge_color}
    `;
    return (<StyledBadge className="dev-badge">{config.badge_text}</StyledBadge>);
  }

  return AppConfig.gl2DevMode()
    ? <Badge className="dev-badge" bsStyle="danger">DEV</Badge>
    : null;
};

HeaderBadge.propTypes = {
  configuration: PropTypes.object.isRequired,
};

export default connect(HeaderBadge, { configurations: ConfigurationsStore }, ({ configurations, ...otherProps }) => ({
  ...configurations,
  ...otherProps,
}));
