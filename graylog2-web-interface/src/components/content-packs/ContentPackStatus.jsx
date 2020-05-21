import PropTypes from 'prop-types';
import React from 'react';
import styled from 'styled-components';
import { Link } from 'react-router';

import { util } from 'theme';
import { StyledBadge } from 'components/graylog/Badge';
import Routes from 'routing/Routes';

const StatusBadge = styled(StyledBadge)(({ status, theme }) => {
  const { success, info, warning, danger } = theme.colors.variant.dark;
  const statuses = {
    installed: success,
    updatable: info,
    edited: warning,
    error: danger,
  };

  return `
    margin-left: 4px;
    background-color: ${statuses[status]};
    color: ${util.readableColor(statuses[status])};
  `;
});

const ContentPackStatus = ({ contentPackId, states }) => {
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

ContentPackStatus.propTypes = {
  states: PropTypes.arrayOf(PropTypes.string),
  contentPackId: PropTypes.string.isRequired,
};

ContentPackStatus.defaultProps = {
  states: [],
};

export default ContentPackStatus;
