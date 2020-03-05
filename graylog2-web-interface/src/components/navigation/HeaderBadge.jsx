import React, { useEffect } from 'react';
import PropTypes from 'prop-types';

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
    return (<Badge className="dev-badge" style={{ backgroundColor: config.badge_color }}>{config.badge_text}</Badge>);
  }

  return AppConfig.gl2DevMode()
    ? <Badge className={`dev-badge danger`}>DEV</Badge>
    : null;
};

HeaderBadge.propTypes = {
  configuration: PropTypes.object.isRequired,
};

export default connect(HeaderBadge, { configurations: ConfigurationsStore }, ({ configurations, ...otherProps }) => ({
  ...configurations,
  ...otherProps,
}));
