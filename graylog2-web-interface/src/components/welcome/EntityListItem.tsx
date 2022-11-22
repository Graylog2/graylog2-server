import React from 'react';
import styled from 'styled-components';

import { Link } from 'components/common/router';
import Routes from 'routing/Routes';
import { ListGroupItem, Label } from 'components/bootstrap';
import { typeLinkMap } from 'components/welcome/helpers';
import type { LastOpenedItem } from 'components/welcome/types';

const StyledListGroupItem = styled(ListGroupItem)`
  display: flex;
  gap: 16px;
`;

export const StyledLabel = styled(Label)`
  cursor: default;
  width: 85px;
  display: block;
`;

const EntityItem = ({ type, title, id }: LastOpenedItem) => {
  return (
    <StyledListGroupItem>
      <StyledLabel bsStyle="info">{type}</StyledLabel>
      <Link target="_blank" to={Routes.pluginRoute(typeLinkMap[type].link)(id)}>{title}</Link>
    </StyledListGroupItem>
  );
};

export default EntityItem;
