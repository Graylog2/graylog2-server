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
import { useTranslation } from 'react-i18next';

import { TimeUnit } from 'components/common';

const DATABASE_TYPES = {
  MAXMIND_ASN: 'maxmind_adapter.database_types.asn',
  MAXMIND_CITY: 'maxmind_adapter.database_types.city',
  MAXMIND_COUNTRY: 'maxmind_adapter.database_types.country',
  IPINFO_STANDARD_LOCATION: 'maxmind_adapter.database_types.ipinfo_location',
  IPINFO_ASN: 'maxmind_adapter.database_types.ipinfo_asn',
};

type Props = {
  dataAdapter: any;
};

const MaxmindAdapterSummary = ({ dataAdapter: { config } }: Props) => {
  const { t } = useTranslation('common');
  const databaseTypeKey = DATABASE_TYPES[config.database_type];

  return (
    <dl>
      <dt>{t('maxmind_adapter.summary.database_file_path')}</dt>
      <dd>{config.path}</dd>
      <dt>{t('maxmind_adapter.summary.database_type')}</dt>
      <dd>{databaseTypeKey ? t(databaseTypeKey) : null}</dd>
      <dt>{t('maxmind_adapter.summary.check_interval')}</dt>
      <dd>
        <TimeUnit value={config.check_interval} unit={config.check_interval_unit} />
      </dd>
    </dl>
  );
};

export default MaxmindAdapterSummary;
