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

import { Button, ButtonToolbar, Col, Row } from 'components/bootstrap';
import { Spinner } from 'components/common';
import useOutdatedIndices from 'components/indices/hooks/useOutdatedIndices';

import useOpenSearchClusterStats from './hooks/useOpenSearchClusterStats';
import { outdatedIndicesMockOverride } from './mockOutdatedIndices';
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
      width: 240px;
    }

    > dd {
      margin-bottom: ${theme.spacings.sm};
      margin-left: 240px;
    }
  `,
);

const MIN_NODES_FOR_ROLLING_UPGRADE = 3;

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

const OpenSearchUpgradeSection = () => {
  const { currentVersion, targetVersion, numberOfDataNodes, isLoading } = useOpenSearchClusterStats();
  const { data: outdatedIndices } = useOutdatedIndices({ mockData: outdatedIndicesMockOverride });
  const isRollingUpgradePossible = numberOfDataNodes >= MIN_NODES_FOR_ROLLING_UPGRADE;
  const hasOutdatedIndices = outdatedIndices.length > 0;

  return (
    <Section>
      <h1>Upgrade Data Node&apos;s embedded OpenSearch</h1>
      <OpenSearchUpgradeInfo currentVersion={currentVersion} targetVersion={targetVersion} isLoading={isLoading} />

      <Row>
        <Col xs={12}>
          <OutdatedIndicesTable />
        </Col>
      </Row>

      <Row>
        <Col xs={12}>
          <ButtonToolbar>
            {isRollingUpgradePossible ? (
              <Button bsStyle="primary" onClick={() => {}} disabled={hasOutdatedIndices} type="button">
                Start OpenSearch Rolling Upgrade
              </Button>
            ) : (
              <Button bsStyle="default" onClick={() => {}} disabled={hasOutdatedIndices} type="button">
                Apply OpenSearch upgrade on next restart
              </Button>
            )}
          </ButtonToolbar>
          {hasOutdatedIndices && <DisabledHint>Resolve all outdated indices first.</DisabledHint>}
        </Col>
      </Row>
    </Section>
  );
};

export default OpenSearchUpgradeSection;
