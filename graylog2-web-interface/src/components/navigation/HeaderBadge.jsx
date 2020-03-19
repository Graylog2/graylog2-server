import React, { useEffect, useState } from 'react';
import styled from 'styled-components';

import CombinedProvider from 'injection/CombinedProvider';
import AppConfig from 'util/AppConfig';
import { Badge } from 'components/graylog';
import connect from 'stores/connect';

const { ConfigurationsActions, ConfigurationsStore } = CombinedProvider.get('Configurations');

const CUSTOMIZATION_CONFIG = 'org.graylog2.configuration.Customization';

const HeaderBadge = () => {
  const [badgeEnabled, setBadgeEnabled] = useState(false);
  const [badgeConfig, setBadgeConfig] = useState({});

  useEffect(() => {
    ConfigurationsActions.list(CUSTOMIZATION_CONFIG).then((configs) => {
      setBadgeEnabled(configs.badge_enable);
      setBadgeConfig(configs);
    });
  }, []);

  if (badgeEnabled) {
    const StyledBadge = styled(Badge)`
      background-color: ${badgeConfig.badge_color};
    `;

    return (<StyledBadge className="dev-badge">{badgeConfig.badge_text}</StyledBadge>);
  }

  return AppConfig.gl2DevMode()
    ? <Badge className="dev-badge" bsStyle="danger">DEV</Badge>
    : null;
};

export default connect(HeaderBadge, { configurations: ConfigurationsStore }, ({ configurations, ...otherProps }) => ({
  ...configurations,
  ...otherProps,
}));
