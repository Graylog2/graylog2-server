/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import PropTypes from 'prop-types';
import React from 'react';
import styled, { css } from 'styled-components';

import { Link } from 'components/graylog/router';
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

  return css`
    margin-left: 4px;
    background-color: ${statuses[status]};
    color: ${theme.utils.readableColor(statuses[status])};
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
