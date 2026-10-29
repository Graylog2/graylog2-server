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

const WhoisAdapterDocumentation = (): React.ReactElement => {
  const { t } = useTranslation('integrations');
  const style: React.CSSProperties = { marginBottom: 10 };

  return (
    <div>
      <p style={style}>{t('whois_adapter_documentation.intro')}</p>

      <h3 style={style}>{t('whois_adapter_documentation.configuration_title')}</h3>

      <h5 style={style}>{t('whois_adapter_field_set.connect_timeout_label')}</h5>

      <p style={style}>{t('whois_adapter_documentation.connect_timeout_description')}</p>

      <h5 style={style}>{t('whois_adapter_field_set.read_timeout_label')}</h5>

      <p style={style}>{t('whois_adapter_documentation.read_timeout_description')}</p>
    </div>
  );
};

export default WhoisAdapterDocumentation;
