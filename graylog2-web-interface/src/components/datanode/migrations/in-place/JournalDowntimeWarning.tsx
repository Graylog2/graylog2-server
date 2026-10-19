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
import { Trans, useTranslation } from 'react-i18next';

import { Alert } from 'components/bootstrap';
import type { MigrationStepComponentProps } from 'components/datanode/Types';
import MigrationStepTriggerButtonToolbar from 'components/datanode/migrations/common/MigrationStepTriggerButtonToolbar';
import useProductName from 'brand-customization/useProductName';

import useJournalDowntimeSize from '../../hooks/useJournalDowntimeSize';
import MigrationError from '../common/MigrationError';

const DownsizeWarning = styled(Alert)`
  margin-top: 10px;
  margin-bottom: 5px;
`;

const JournalDowntimeWarning = ({ currentStep, onTriggerStep, hideActions }: MigrationStepComponentProps) => {
  const { t } = useTranslation('datanode');
  const productName = useProductName();
  const { data, error, isError } = useJournalDowntimeSize();

  return (
    <>
      <h3>{t('journal_downtime.title')}</h3>
      <p>{t('journal_downtime.processing_stops', { productName })}</p>
      <p>{t('journal_downtime.increase_size')}</p>
      <p>
        <Trans
          t={t}
          i18nKey="journal_downtime.current_size"
          values={{ size: data.journal_size_MB, throughput: data.KBs_per_minute }}
          components={{ bold: <b /> }}
        />
      </p>
      <p>
        <Trans
          t={t}
          i18nKey="journal_downtime.max_downtime"
          values={{ productName, duration: data.max_downtime_duration }}
          components={{ bold: <b /> }}
        />
      </p>
      {isError && <MigrationError errorMessage={t('journal_downtime.estimation_error', { message: error?.message })} />}
      <DownsizeWarning bsStyle="warning">{t('journal_downtime.sufficient_size')}</DownsizeWarning>
      <MigrationStepTriggerButtonToolbar
        hidden={hideActions}
        nextSteps={currentStep.next_steps}
        onTriggerStep={onTriggerStep}
      />
    </>
  );
};

export default JournalDowntimeWarning;
