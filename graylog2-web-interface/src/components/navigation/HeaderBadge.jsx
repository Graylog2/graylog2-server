import React from 'react';

import AppConfig from 'util/AppConfig';
import { Badge } from 'components/graylog';
import { PluginStore } from 'graylog-web-plugin/plugin';

const HeaderBadge = () => {
  const PluginHeader = (PluginStore.exports('navigation')[0] || {}).badgeComponent || <span />;

  const devBadge = AppConfig.gl2DevMode() ? <Badge className="dev-badge" bsStyle="danger">DEV</Badge> : null;
  return (
    <>
      {devBadge}
      <PluginHeader />
    </>
  );
};

export default HeaderBadge;
