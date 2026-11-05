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

import { Panel } from 'components/bootstrap';

import styles from './TemplateFieldValueProviderPreview.css';

const TemplateFieldValueProviderPreview = () => {
  const { t } = useTranslation('event_definitions');

  return (
    <Panel className={styles.templatePreview} header={<h3>{t('template_preview.title')}</h3>}>
      <p>
        <Trans
          t={t}
          i18nKey="template_preview.description"
          // eslint-disable-next-line no-template-curly-in-string
          values={{ syntax: '${source.<fieldName>}' }}
          components={{ code: <code /> }}
        />
        <br />
        {t('template_preview.available_fields')}
      </p>
      <ul>
        <li>
          <b>{t('template_preview.filter_label')}</b> {t('template_preview.filter')}
        </li>
        <li>
          <b>{t('template_preview.aggregation_label')}</b> {t('template_preview.aggregation')}
        </li>
        <li>
          <b>{t('template_preview.correlation_label')}</b> {t('template_preview.correlation')}
        </li>
      </ul>
    </Panel>
  );
};

export default TemplateFieldValueProviderPreview;
