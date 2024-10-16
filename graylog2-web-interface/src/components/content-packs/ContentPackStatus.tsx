import React from 'react';
import styled, { css } from 'styled-components';

import Badge from 'components/bootstrap/Badge';
import { Link } from 'components/common/router';
import Routes from 'routing/Routes';

const StatusBadge = styled(Badge)(({ status, theme }) => {
  const { success, info, warning, danger } = theme.colors.variant.dark;
  const statuses = {
    installed: success,
    updatable: info,
    edited: warning,
    error: danger,
  };

  return css`
    margin-left: 4px;
    background-color: ${statuses[status]};
    color: ${theme.utils.readableColor(statuses[status])};
`;
});

type ContentPackStatusProps = {
  states?: string[];
  contentPackId: string;
};

const ContentPackStatus = ({
  contentPackId,
  states = []
}: ContentPackStatusProps) => {
  const badges = states.map((state) => (
    <Link key={state} to={Routes.SYSTEM.CONTENTPACKS.show(contentPackId)}>
      <StatusBadge status={state}>{state}</StatusBadge>
    </Link>
  ));

  return (
    <span>
      {badges}
    </span>
  );
};

export default ContentPackStatus;
