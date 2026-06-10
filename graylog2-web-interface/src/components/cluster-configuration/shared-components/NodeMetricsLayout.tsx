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
import styled from 'styled-components';

import { Label } from 'components/bootstrap';

export const MetricsColumn = styled.div`
  display: flex;
  flex-direction: column;
  gap: 4px;
`;

export const MetricsRow = styled.div`
  font-size: small;
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;

  span {
    font-size: inherit;
  }
`;

export const SecondaryText = styled.div`
  font-size: small;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;

  span {
    font-size: inherit;
  }
`;

export const NodePrimary = styled.div`
  display: flex;
  flex-direction: column;
  gap: 2px;
`;

export const StyledLabel = styled(Label)`
  display: inline-flex;
`;

export const RoleLabel = styled(Label)`
  display: inline-flex;
  justify-content: center;
  gap: 4px;
`;

export const MetricPlaceholder = () => (
  <MetricsColumn>
    <SecondaryText>
      <span>N/A</span>
    </SecondaryText>
  </MetricsColumn>
);
