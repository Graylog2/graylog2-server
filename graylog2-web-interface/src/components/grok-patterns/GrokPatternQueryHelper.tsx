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
import { useTranslation } from 'react-i18next';

import QueryHelper from 'components/common/QueryHelper';

// Query syntax, so it stays untranslated.
const QUERY_EXAMPLE = 'pattern:COMMON';

const GrokPatternQueryHelper = () => {
  const { t } = useTranslation('grokpatterns');

  const fieldMap = {
    pattern: t('query_helper_field_pattern'),
  };

  const queryExample = (
    <p>
      {t('query_helper_example')}
      <br />
      <kbd>{QUERY_EXAMPLE}</kbd>
      <br />
    </p>
  );

  return (
    <QueryHelper
      entityName={t('query_helper_entity_name')}
      commonFields={['name']}
      fieldMap={fieldMap}
      example={queryExample}
    />
  );
};

export default GrokPatternQueryHelper;
