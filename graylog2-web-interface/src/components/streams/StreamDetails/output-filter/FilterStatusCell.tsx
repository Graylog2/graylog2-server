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
import * as React from 'react';
import styled, { css } from 'styled-components';

import { Label } from 'components/bootstrap';

import type { StreamOutputFilterRule } from './Types';

const StatusLabel = styled(Label)<{ $clickable: boolean }>(({ $clickable }) => css`
  cursor: ${$clickable ? 'pointer' : 'default'};
  display: inline-flex;
  justify-content: center;
  gap: 4px;
`);

type Props = {
  filterOutputRule: StreamOutputFilterRule,
};

const FilterStatusCell = ({ filterOutputRule }: Props) => {
  const isEnabled = filterOutputRule.status === 'enabled';
  const title = isEnabled ? 'Enabled' : 'Disabled';

  return (
    <StatusLabel bsStyle={isEnabled ? 'success' : 'warning'}
                 title={title}
                 aria-label={title}
                 role="button"
                 $clickable={false}>
      {title}
    </StatusLabel>
  );
};

export default FilterStatusCell;
