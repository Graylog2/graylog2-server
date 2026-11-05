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

import { FormikInput } from 'components/common';

const IndexSetReadOnlyConfiguration = ({
  hiddenFields,
  immutableFields,
  ignoreFieldRestrictions,
  onPrefixChange,
}: {
  hiddenFields: string[];
  immutableFields: string[];
  ignoreFieldRestrictions: boolean;
  onPrefixChange: () => void;
}) => {
  const { t } = useTranslation('indexsets');

  const _validateIndexPrefix = (value: string) => {
    let error: string;

    if (value?.length === 0) {
      error = t('index_set_form.prefix_error_empty');
    } else if (value?.indexOf('_') === 0 || value?.indexOf('-') === 0 || value?.indexOf('+') === 0) {
      error = t('index_set_form.prefix_error_start');
    } else if (value?.toLocaleLowerCase() !== value) {
      error = t('index_set_form.prefix_error_lower_case');
    } else if (!value?.match(/^[a-z0-9][a-z0-9_\-+]*$/)) {
      error = t('index_set_form.prefix_error_chars');
    }

    return error;
  };

  const indexPrefixHelp = (
    <span>
      <Trans t={t} i18nKey="index_set_form.prefix_help" components={{ strong: <strong /> }} />
    </span>
  );

  return (
    <>
      <FormikInput
        type="text"
        id="index-prefix"
        label={t('index_set_form.prefix_label')}
        name="index_prefix"
        help={indexPrefixHelp}
        validate={_validateIndexPrefix}
        onChange={onPrefixChange}
        required
      />
      {(!hiddenFields.includes('index_analyzer') || ignoreFieldRestrictions) && (
        <FormikInput
          type="text"
          id="index-analyzer"
          label={t('index_set_form.analyzer_label')}
          name="index_analyzer"
          help={t('index_set_form.analyzer_help')}
          required
          disabled={immutableFields.includes('index_analyzer') && !ignoreFieldRestrictions}
        />
      )}
    </>
  );
};

export default IndexSetReadOnlyConfiguration;
