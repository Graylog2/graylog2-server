import React, { useContext } from 'react';
import styled from 'styled-components';

import AppConfig from 'util/AppConfig';
import { Badge } from 'components/graylog';
import { CustomUiContext } from 'contexts/CustomUi';

const HeaderBadge = () => {
  const { badgeConfig } = useContext(CustomUiContext);

  if (badgeConfig.badge_enable) {
    const StyledBadge = styled(Badge)`
      background-color: ${badgeConfig.badge_color};
    `;

    return (<StyledBadge className="dev-badge">{badgeConfig.badge_text}</StyledBadge>);
  }

  return AppConfig.gl2DevMode()
    ? <Badge className="dev-badge" bsStyle="danger">DEV</Badge>
    : null;
};

export default HeaderBadge;
