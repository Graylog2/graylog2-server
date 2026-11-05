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

import { MantineAccordion } from 'components/bootstrap';
import useProductName from 'brand-customization/useProductName';

const CaffeineCacheDocumentation = () => {
  const { t } = useTranslation('lookup_tables');
  const productName = useProductName();

  const accordionItems = [
    {
      value: 'cache_size',
      label: t('caffeine_doc.cache_size_label'),
      content: <p>{t('caffeine_doc.cache_size_text')}</p>,
    },
    {
      value: 'time_based_expiration',
      label: t('caffeine_doc.time_based_label'),
      content: (
        <div>
          <h5 style={{ marginBottom: 10 }}>{t('caffeine_doc.expire_access_title')}</h5>
          <p style={{ marginBottom: 10, padding: 0 }}>
            {t('caffeine_doc.expire_access_text')}
            <br />
            {t('caffeine_doc.expire_access_result')}
          </p>

          <h5 style={{ marginBottom: 10 }}>{t('caffeine_doc.expire_write_title')}</h5>
          <p style={{ marginBottom: 10, padding: 0 }}>
            {t('caffeine_doc.expire_write_text')}
            <br />
            {t('caffeine_doc.expire_write_result')}
          </p>
        </div>
      ),
    },
  ];

  return (
    <div>
      <p>{t('caffeine_doc.intro')}</p>
      <p>{t('caffeine_doc.heap_hint', { productName })}</p>

      <p>{t('caffeine_doc.local_hint', { productName })}</p>
      <p>{t('caffeine_doc.example')}</p>

      <hr />

      <MantineAccordion accordionItems={accordionItems} defaultValue="cache_size" />
    </div>
  );
};

export default CaffeineCacheDocumentation;
