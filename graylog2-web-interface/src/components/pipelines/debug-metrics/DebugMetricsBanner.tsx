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

import { Alert, Button } from 'components/bootstrap';
import IfPermitted from 'components/common/IfPermitted';

import useDebugMetricsConfig, { type UseDebugMetricsConfig } from './useDebugMetricsConfig';

const EDIT_PERMISSION = 'pipeline:edit';

const BannerActions = styled.div(
  ({ theme }) => css`
    display: flex;
    gap: ${theme.spacings.sm};
    margin-top: ${theme.spacings.xs};
  `,
);

type Props = {
  config?: UseDebugMetricsConfig;
};

const DebugMetricsBanner = ({ config = undefined }: Props) => {
  const hookConfig = useDebugMetricsConfig({ loadOnMount: config === undefined });
  const { metricsEnabled, isLoading, disable } = config ?? hookConfig;

  if (isLoading || !metricsEnabled) {
    return null;
  }

  return (
    <Alert bsStyle="warning" title="Debug metrics are enabled">
      <p>
        Debug metrics add overhead to message processing and increase memory usage. Disable them when you are finished
        troubleshooting. Pipeline Load values are smoothed over a 15-minute window and may take a few minutes to
        stabilize after enabling.
      </p>
      <IfPermitted permissions={[EDIT_PERMISSION]}>
        <BannerActions>
          <Button bsStyle="warning" bsSize="small" onClick={disable}>
            Disable debug metrics
          </Button>
        </BannerActions>
      </IfPermitted>
    </Alert>
  );
};

export default DebugMetricsBanner;
