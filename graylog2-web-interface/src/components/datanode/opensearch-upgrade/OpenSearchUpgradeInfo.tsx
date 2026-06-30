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

import { Spinner } from 'components/common';

const InfoList = styled.dl(
  ({ theme }) => css`
    margin: ${theme.spacings.md} 0;

    > dt {
      clear: left;
      float: left;
      margin-bottom: ${theme.spacings.sm};
      width: 240px;
    }

    > dd {
      margin-bottom: ${theme.spacings.sm};
      margin-left: 240px;
    }
  `,
);

const OpenSearchUpgradeInfo = ({
  currentVersion,
  targetVersion,
  isLoading,
}: {
  currentVersion: string | undefined;
  targetVersion: string | undefined;
  isLoading: boolean;
}) => (
  <InfoList>
    <dt>Current OpenSearch version:</dt>
    <dd>{isLoading ? <Spinner text="Loading..." /> : currentVersion || 'Unknown'}</dd>
    <dt>Target OpenSearch version:</dt>
    <dd>{isLoading ? <Spinner text="Loading..." /> : targetVersion || 'Unknown'}</dd>
  </InfoList>
);

export default OpenSearchUpgradeInfo;
