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
import React, { useEffect } from 'react';
import styled from 'styled-components';

import { Row, Col } from 'components/bootstrap';
import { DocumentTitle, PageHeader } from 'components/common';
import DocsHelper from 'util/DocsHelper';
import DataNodesPageNavigation from 'components/datanode/DataNodePageNavigation';
import MigrationWizard from 'components/datanode/migrations/MigrationWizard';
import useMigrationWizardStep from 'components/datanode/hooks/useMigrationWizardStep';
import useTriggerMigrationState from 'components/datanode/hooks/useTriggerMigrationState';
import { MIGRATION_STATE } from 'components/datanode/Constants';
import ResetMigrationButton from 'components/datanode/migrations/common/ResetMigrationButton';

const WizardContainer = styled(Col)`
  .nav-pills > li > a  {
    pointer-events: none;
  }
`;

const DataNodesMigrationPage = () => {
  const { step: currentStep, isLoading } = useMigrationWizardStep();
  const { onTriggerNextState } = useTriggerMigrationState();

  useEffect(() => {
    if (!isLoading && currentStep.state === MIGRATION_STATE.NEW.key) {
      onTriggerNextState({ step: currentStep.next_steps[0], args: {} });
    }
  }, [currentStep.next_steps, currentStep.state, isLoading, onTriggerNextState]);

  return (
    <DocumentTitle title="Data Nodes Migration">
      <DataNodesPageNavigation />
      <PageHeader title="Data Nodes Migration"
                  actions={<ResetMigrationButton />}
                  documentationLink={{
                    title: 'Data Nodes documentation',
                    path: DocsHelper.PAGES.GRAYLOG_DATA_NODE,
                  }}>
        <span>
          Graylog Data Nodes offer a better integration with Graylog and simplify future updates. They allow you to index and search through all the messages in your Graylog message database.
        </span>
      </PageHeader>
      <Row className="content">
        <WizardContainer md={12}>
          <MigrationWizard />
        </WizardContainer>
      </Row>
    </DocumentTitle>
  );
};

export default DataNodesMigrationPage;
