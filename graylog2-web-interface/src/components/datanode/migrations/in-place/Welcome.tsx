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
import { Trans, useTranslation } from 'react-i18next';

import { Col, Row } from 'components/bootstrap';
import { DocumentationLink } from 'components/support';
import MigrationDatanodeList from 'components/datanode/migrations/MigrationDatanodeList';
import useDataNodes from 'components/datanode/hooks/useDataNodes';
import type { MigrationStepComponentProps } from 'components/datanode/Types';
import MigrationStepTriggerButtonToolbar from 'components/datanode/migrations/common/MigrationStepTriggerButtonToolbar';
import JournalSizeWarning from 'components/datanode/migrations/in-place/JournalSizeWarning';
import InPlaceMigrationInfo from 'components/datanode/migrations/common/InPlaceMigrationInfo';
import JwtAuthenticationInfo from 'components/datanode/migrations/common/JwtAuthenticationInfo';

const DataNodeDocsLink = ({ children = undefined }: React.PropsWithChildren) => (
  <DocumentationLink page="graylog-data-node" text={children} />
);

const Welcome = ({ currentStep, onTriggerStep, hideActions }: MigrationStepComponentProps) => {
  const { t } = useTranslation('datanode');
  const { data: dataNodes } = useDataNodes();

  return (
    <>
      <Row>
        <Col md={6}>
          <h3>{t('in_place_welcome.title')}</h3>
          <p>{t('in_place_welcome.description')}</p>
          <p>
            <Trans t={t} i18nKey="in_place_welcome.install" components={{ docsLink: <DataNodeDocsLink /> }} />
          </p>
          <MigrationDatanodeList />
        </Col>
        <Col md={6}>
          <JournalSizeWarning />
          <InPlaceMigrationInfo />
          <JwtAuthenticationInfo />
        </Col>
      </Row>
      <MigrationStepTriggerButtonToolbar
        hidden={hideActions}
        disabled={dataNodes?.list?.length <= 0}
        nextSteps={currentStep.next_steps}
        onTriggerStep={onTriggerStep}
      />
    </>
  );
};

export default Welcome;
