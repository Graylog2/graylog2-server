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
import { Trans, useTranslation } from 'react-i18next';

import type { MigrationStepComponentProps } from 'components/datanode/Types';
import MigrationStepTriggerButtonToolbar from 'components/datanode/migrations/common/MigrationStepTriggerButtonToolbar';
import { Space } from 'preflight/components/common';
import MigrationDatanodeList from 'components/datanode/migrations/MigrationDatanodeList';
import useProductName from 'brand-customization/useProductName';

const EXAMPLE_HOSTS_CONFIGURATION =
  'elasticsearch_hosts = https://admin:admin@opensearch1:9200,https://admin:admin@opensearch2:9200,https://admin:admin@opensearch3:9200';

const RestartGraylog = ({ currentStep, onTriggerStep, hideActions }: MigrationStepComponentProps) => {
  const { t } = useTranslation('datanode');
  const productName = useProductName();

  return (
    <>
      <p>{t('restart_graylog.almost_there')}</p>
      <p>
        <Trans t={t} i18nKey="restart_graylog.remove_hosts" components={{ codeSnippet: <code /> }} />
      </p>
      <p>
        {t('restart_graylog.example')} <code>{EXAMPLE_HOSTS_CONFIGURATION}</code>
      </p>
      <Space h="md" />
      <MigrationDatanodeList showProvisioningState={false} />
      <p>{t('restart_graylog.wait_for_available')}</p>
      <p>{t('restart_graylog.restart', { productName })}</p>
      <MigrationStepTriggerButtonToolbar
        hidden={hideActions}
        nextSteps={currentStep.next_steps}
        onTriggerStep={onTriggerStep}
      />
    </>
  );
};

export default RestartGraylog;
