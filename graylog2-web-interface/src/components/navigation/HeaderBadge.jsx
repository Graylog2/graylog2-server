import React from 'react';
import createReactClass from 'create-react-class';
import Reflux from 'reflux';
import CombinedProvider from 'injection/CombinedProvider';
import { Badge } from 'react-bootstrap';
import AppConfig from 'util/AppConfig';
import badgeStyles from 'components/bootstrap/Badge.css';

const { ConfigurationsActions, ConfigurationsStore } = CombinedProvider.get('Configurations');

const HeaderBadge = createReactClass({
  displayName: 'HeaderBadge',
  mixins: [Reflux.connect(ConfigurationsStore)],

  componentDidMount() {
    ConfigurationsActions.list(this.CUSTOMIZATION_CONFIG);
  },

  CUSTOMIZATION_CONFIG: 'org.graylog2.configuration.Customization',

  render() {
    const { configuration = {} } = this.state;
    const config = configuration[this.CUSTOMIZATION_CONFIG];
    const badgeEnabled = config && config.badge_enable;

    if (badgeEnabled) {
      return (<Badge className="dev-badge" style={{ backgroundColor: config.badge_color }}>{config.badge_text}</Badge>);
    }

    return AppConfig.gl2DevMode()
      ? <Badge className={`dev-badge ${badgeStyles.badgeDanger}`}>DEV</Badge>
      : null;
  },
});

export default HeaderBadge;
