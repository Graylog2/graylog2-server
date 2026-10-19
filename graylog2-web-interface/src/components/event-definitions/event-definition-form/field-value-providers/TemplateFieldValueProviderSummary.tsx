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

import CommonFieldValueProviderSummary from './CommonFieldValueProviderSummary';

type Props = React.ComponentProps<FieldValueProvider['summaryComponent']>;

const TemplateFieldValueProviderSummary = ({ ...props }: Props) => {
  const { t } = useTranslation('event_definitions');
  const { config } = props;

  return (
    <CommonFieldValueProviderSummary {...props}>
      <>
        <tr>
          <td>{t('provider_summary.value_source')}</td>
          <td>{t('field_value_providers.template.title')}</td>
        </tr>
        <tr>
          <td>{t('field_value_providers.template.title')}</td>
          <td>{config.providers[0].template}</td>
        </tr>
        <tr>
          <td>{t('provider_summary.validate_template_values')}</td>
          <td>{config.providers[0].require_values ? t('fields_list.yes') : t('fields_list.no')}</td>
        </tr>
        <tr>
          <td>{t('template_provider_form.exclude_empty_fields')}</td>
          <td>{config.providers[0].exclude_empty_fields ? t('fields_list.yes') : t('fields_list.no')}</td>
        </tr>
      </>
    </CommonFieldValueProviderSummary>
  );
};

export default TemplateFieldValueProviderSummary;
