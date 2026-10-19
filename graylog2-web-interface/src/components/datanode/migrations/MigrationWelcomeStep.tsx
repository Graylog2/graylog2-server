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
import { Trans, useTranslation } from 'react-i18next';

import { Col, Row, Panel, Alert } from 'components/bootstrap';
import { DocumentationLink } from 'components/support';
import MigrationDatanodeList from 'components/datanode/migrations/MigrationDatanodeList';
import MigrationStepTriggerButtonToolbar from 'components/datanode/migrations/common/MigrationStepTriggerButtonToolbar';
import type { MigrationStepComponentProps } from 'components/datanode/Types';
import MigrationError from 'components/datanode/migrations/common/MigrationError';
import useProductName from 'brand-customization/useProductName';
import useDataNodes from 'components/datanode/hooks/useDataNodes';

import useIsElasticsearch from '../hooks/useIsElasticsearch';

const Headline = styled.h2`
  margin-top: 5px;
  margin-bottom: 10px;
`;

export const StyledPanel = styled(Panel)<{ bsStyle: string }>(
  ({ bsStyle = 'default', theme }) => css`
    &.panel {
      background-color: ${theme.colors.global.contentBackground};

      .panel-heading {
        color: ${theme.colors.variant.darker[bsStyle]};
      }
    }
  `,
);

const DataNodeDocsLink = ({ children = undefined }: React.PropsWithChildren) => (
  <DocumentationLink page="graylog-data-node" text={children} />
);

const MigrationWelcomeStep = ({ currentStep, onTriggerStep, hideActions }: MigrationStepComponentProps) => {
  const { t } = useTranslation('datanode');
  const isElasticsearch = useIsElasticsearch();
  const productName = useProductName();
  const { data: dataNodes } = useDataNodes();

  return (
    <Row>
      <Col md={12}>
        {isElasticsearch && <Alert bsStyle="warning">{t('migration_welcome.incompatible_backend')}</Alert>}
        <MigrationError errorMessage={currentStep.error_message} />
        <Headline>{t('migration_welcome.title')}</Headline>
        <p>{t('migration_welcome.intro', { productName })}</p>
        <p>{t('migration_welcome.earlier_deployments')}</p>
        <p>
          {t('migration_welcome.tool_description')}
          <br />
        </p>
        <p>{t('migration_welcome.required_steps')}</p>
        <p>
          <Trans t={t} i18nKey="migration_welcome.more_information" components={{ docsLink: <DataNodeDocsLink /> }} />
        </p>
        <br />
        <MigrationDatanodeList />
        {!isElasticsearch && (
          <MigrationStepTriggerButtonToolbar
            hidden={hideActions}
            disabled={dataNodes.list.length === 0}
            nextSteps={currentStep.next_steps}
            onTriggerStep={onTriggerStep}
          />
        )}
      </Col>
    </Row>
  );
};

export default MigrationWelcomeStep;
