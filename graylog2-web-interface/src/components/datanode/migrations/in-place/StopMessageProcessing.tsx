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
import styled from 'styled-components';
import { useTranslation } from 'react-i18next';

import MigrationStepTriggerButtonToolbar from 'components/datanode/migrations/common/MigrationStepTriggerButtonToolbar';
import type { MigrationStepComponentProps } from 'components/datanode/Types';
import { Panel } from 'components/bootstrap';
import { Icon } from 'components/common';
import { StyledPanel } from 'components/datanode/migrations/MigrationWelcomeStep';
import useProductName from 'brand-customization/useProductName';

const StyledHelpPanel = styled(StyledPanel)`
  margin-top: 30px;
`;

const StopMessageProcessing = ({ currentStep, onTriggerStep, hideActions }: MigrationStepComponentProps) => {
  const { t } = useTranslation('datanode');
  const productName = useProductName();

  return (
    <>
      <p>{t('stop_processing.stopped', { productName })}</p>
      <StyledHelpPanel bsStyle="warning">
        <Panel.Heading>
          <Panel.Title componentClass="h3">
            <Icon name="warning" />
            {t('stop_processing.stop_opensearch')}
          </Panel.Title>
        </Panel.Heading>
        <Panel.Body>
          <p>{t('stop_processing.stop_cluster')}</p>
          <p>{t('stop_processing.change_owner')}</p>
          <p>
            {/* eslint-disable-next-line graylog/no-untranslated-text -- shell command */}
            <code>sudo chown -R graylog-datanode:graylog-datanode &lt;your_data_directory&gt;</code>
            <br />
            {/* eslint-disable-next-line graylog/no-untranslated-text -- shell command */}
            <code>sudo chmod -R 750 &lt;your_data_directory&gt;</code>
          </p>
        </Panel.Body>
      </StyledHelpPanel>
      <p />
      <MigrationStepTriggerButtonToolbar
        hidden={hideActions}
        nextSteps={currentStep.next_steps}
        onTriggerStep={onTriggerStep}
      />
    </>
  );
};

export default StopMessageProcessing;
