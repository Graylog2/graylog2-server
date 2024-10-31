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
import React from 'react';
import styled, { css } from 'styled-components';

import Routes from 'routing/Routes';
import { Link } from 'components/common/router';
import { Badge } from 'components/bootstrap';

type Props = {
  title: string,
  id: string,
  isDefault: boolean,
  isEnabled: boolean,
}

const DisabledTitle = styled.span(({ theme }) => css`
  color: ${theme.colors.global.textSecondary};
`);

const StyledBadge = styled(Badge)(({ theme }) => css`
  margin-left: ${theme.spacings.xs};
`);

const TitleCell = ({ title, id, isDefault, isEnabled }: Props) => {
  if (!isEnabled) {
    return (
      <>
        <DisabledTitle>{title} (disabled)</DisabledTitle>
        {isDefault && (<StyledBadge>Default</StyledBadge>)}
      </>
    );
  }

  return (
    <Link to={Routes.SYSTEM.INDICES.TEMPLATES.view(id)}>
      <span>{title}</span>
      {isDefault && (<StyledBadge>Default</StyledBadge>)}
    </Link>
  );
};

export default TitleCell;
