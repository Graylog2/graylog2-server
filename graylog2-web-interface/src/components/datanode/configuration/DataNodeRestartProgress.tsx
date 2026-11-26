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

import { Link, Spinner } from 'components/common';
import RoutePaths from 'routing/Routes';

const Progress = styled.span`
  display: inline-flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacings.sm};
`;

type Props = { progress: string | undefined; paused?: boolean };

const DataNodeRestartProgress = ({ progress, paused = false }: Props) => {
  if (paused) {
    return (
      <>
        <p role="status">{progress}</p>
        <p>
          Once the cluster is green again,{' '}
          <Link to={RoutePaths.SYSTEM.CLUSTER.DATANODE_UPGRADE}>resume the restart</Link> on the Data Node Upgrade
          page.
        </p>
      </>
    );
  }

  return (
    <Progress>
      <Spinner text="" delay={0} />
      <span role="status">{progress}</span>
    </Progress>
  );
};

export default DataNodeRestartProgress;
