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

import { Alert, SegmentedControl } from 'components/bootstrap';

export type DataNodeUpgradeMethodType = 'cluster-restart' | 'rolling-upgrade';

type Props = {
  upgradeMethod: DataNodeUpgradeMethodType;
  onChange: (value: DataNodeUpgradeMethodType) => void;
};

const UpgradeMethodSelector = ({ upgradeMethod, onChange }: Props) => {
  const { t } = useTranslation('collectors', { keyPrefix: 'datanode' });
  const upgradeMethodSegments: Array<{ value: DataNodeUpgradeMethodType; label: string }> = [
    { value: 'cluster-restart', label: t('upgrade_method.cluster_restart') },
    { value: 'rolling-upgrade', label: t('upgrade_method.rolling_upgrade') },
  ];

  return (
    <>
      <SegmentedControl data={upgradeMethodSegments} value={upgradeMethod} onChange={onChange} />
      <Alert bsStyle="info">
        {upgradeMethod === 'cluster-restart' && (
          <>
            <p>{t('upgrade_method.cluster_restart_description')}</p>
            <p>{t('upgrade_method.cluster_restart_less_than_three')}</p>
            <p>{t('upgrade_method.cluster_restart_three_or_more')}</p>
          </>
        )}
        {upgradeMethod === 'rolling-upgrade' && (
          <>
            <p>
              <Trans t={t} i18nKey="upgrade_method.rolling_upgrade_description" components={{ bold: <b /> }} />
            </p>
            <p>{t('upgrade_method.rolling_upgrade_process')}</p>
          </>
        )}
      </Alert>
    </>
  );
};

export default UpgradeMethodSelector;
