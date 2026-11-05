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
import { Alert } from 'components/bootstrap';

const BLOCKLIST_TYPE = {
  DOMAINS: 'abusech_ransom_adapter_summary.domains_blocklist',
  URLS: 'abusech_ransom_adapter_summary.urls_blocklist',
  IPS: 'abusech_ransom_adapter_summary.ips_blocklist',
};

type Props = {
  dataAdapter: any;
};

const AbuseChRansomAdapterSummary = ({ dataAdapter: { config } }: Props) => {
  const { t } = useTranslation('integrations');
  const blocklistTypeKey = BLOCKLIST_TYPE[config.blocklist_type];

  return (
    <div>
      <dl>
        <dt>{t('abusech_ransom_adapter_summary.blocklist_type_label')}</dt>
        <dd>{blocklistTypeKey && t(blocklistTypeKey)}</dd>
        <dt>{t('abusech_ransom_adapter_summary.update_interval_label')}</dt>
        <dd>
          <TimeUnit value={config.refresh_interval} unit={config.refresh_interval_unit} />
        </dd>
      </dl>
      <Alert
        style={{ marginBottom: 10 }}
        bsStyle="warning"
        title={t('abusech_ransom_adapter_summary.deprecation_title')}>
        <p>{t('abusech_ransom_adapter_summary.deprecation_text')}</p>
      </Alert>
    </div>
  );
};

export default AbuseChRansomAdapterSummary;
