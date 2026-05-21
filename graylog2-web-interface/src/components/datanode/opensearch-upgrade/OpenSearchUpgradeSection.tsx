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
import { useQuery } from '@tanstack/react-query';
import styled, { css } from 'styled-components';

import { SystemClusterStats } from '@graylog/server-api';

import { Button, ButtonToolbar, Col, Row } from 'components/bootstrap';
import { Spinner } from 'components/common';
import useOutdatedIndices from 'components/indices/hooks/useOutdatedIndices';
import { defaultOnError } from 'util/conditional/onError';

import OutdatedIndicesTable from './OutdatedIndicesTable';

const Section = styled.div(
  ({ theme }) => css`
    border-top: 1px solid ${theme.colors.variant.default};
    padding-top: ${theme.spacings.lg};
  `,
);

const DisabledHint = styled.p(
  ({ theme }) => css`
    margin-top: ${theme.spacings.xs};
    margin-bottom: 0;
    color: ${theme.colors.gray[60]};
    font-size: ${theme.fonts.size.small};
  `,
);

const InfoList = styled.dl(
  ({ theme }) => css`
    margin: ${theme.spacings.md} 0;

    > dt {
      clear: left;
      float: left;
      margin-bottom: ${theme.spacings.sm};
      width: 180px;
    }

    > dd {
      margin-bottom: ${theme.spacings.sm};
      margin-left: 180px;
    }
  `,
);

const TARGET_OPENSEARCH_VERSION = '3.5.0';
const MIN_NODES_FOR_ROLLING_UPGRADE = 3;

const useOpenSearchClusterStats = () => {
  const { data, isInitialLoading } = useQuery({
    queryKey: ['opensearch-upgrade', 'cluster-stats'],
    queryFn: () =>
      defaultOnError(
        SystemClusterStats.elasticsearchStats(),
        'Loading OpenSearch cluster stats failed',
        'Could not load OpenSearch cluster stats',
      ),
  });

  return {
    currentVersion: data?.cluster_version,
    numberOfDataNodes: data?.cluster_health?.number_of_data_nodes ?? 0,
    isLoading: isInitialLoading,
  };
};

const OpenSearchUpgradeInfo = ({
  currentVersion,
  isLoading,
}: {
  currentVersion: string | undefined;
  isLoading: boolean;
}) => (
  <InfoList>
    <dt>Current OpenSearch:</dt>
    <dd>{isLoading ? <Spinner text="Loading..." /> : currentVersion || 'Unknown'}</dd>
    <dt>Target OpenSearch:</dt>
    <dd>{TARGET_OPENSEARCH_VERSION}</dd>
  </InfoList>
);

const OpenSearchUpgradeSection = () => {
  const { currentVersion, numberOfDataNodes, isLoading } = useOpenSearchClusterStats();
  const { data: outdatedIndices } = useOutdatedIndices();
  const isRollingUpgradePossible = numberOfDataNodes >= MIN_NODES_FOR_ROLLING_UPGRADE;
  const hasOutdatedIndices = outdatedIndices.length > 0;

  return (
    <Col xs={12}>
      <Section>
        <h3>OpenSearch Upgrade</h3>
        <OpenSearchUpgradeInfo currentVersion={currentVersion} isLoading={isLoading} />

        <Row>
          <Col xs={12}>
            <OutdatedIndicesTable />
          </Col>
        </Row>

        <Row>
          <Col xs={12}>
            <ButtonToolbar>
              <Button bsStyle="default" onClick={() => {}} disabled={hasOutdatedIndices} type="button">
                Apply OpenSearch upgrade on next restart
              </Button>
              {isRollingUpgradePossible && (
                <Button bsStyle="primary" onClick={() => {}} disabled={hasOutdatedIndices} type="button">
                  Start OpenSearch Rolling Upgrade
                </Button>
              )}
            </ButtonToolbar>
            {hasOutdatedIndices && <DisabledHint>Resolve all outdated indices first.</DisabledHint>}
          </Col>
        </Row>
      </Section>
    </Col>
  );
};

export default OpenSearchUpgradeSection;
