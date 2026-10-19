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
import type { FieldValueProvider } from 'graylog-web-plugin';
import { useTranslation } from 'react-i18next';

import { IfPermitted } from 'components/common';

import CommonFieldValueProviderSummary from './CommonFieldValueProviderSummary';

type Props = React.ComponentProps<FieldValueProvider['summaryComponent']>;

const LookupTableFieldValueProviderSummary = ({ ...props }: Props) => {
  const { t } = useTranslation('event_definitions');
  const provider = props.config.providers[0];

  return (
    <CommonFieldValueProviderSummary {...props}>
      <IfPermitted permissions="lookuptables:read">
        <>
          <tr>
            <td>{t('provider_summary.value_source')}</td>
            <td>{t('field_value_providers.lookup_table.title')}</td>
          </tr>
          <tr>
            <td>{t('field_value_providers.lookup_table.title')}</td>
            <td>{provider.table_name}</td>
          </tr>
          <tr>
            <td>{t('provider_summary.lookup_table_key_field')}</td>
            <td>{provider.key_field}</td>
          </tr>
        </>
      </IfPermitted>
    </CommonFieldValueProviderSummary>
  );
};

export default LookupTableFieldValueProviderSummary;
