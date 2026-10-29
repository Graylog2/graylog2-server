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
import styled from 'styled-components';
import { useField } from 'formik';

import { defaultCompare as naturalSort } from 'logic/DefaultCompare';
import { Input, Button } from 'components/bootstrap';
import { Select } from 'components/common';
import type { LookupTableAdapter } from 'logic/lookup-tables/types';

const StyledSelect = styled(Select)`
  flex: 1 1 auto;
  min-width: 0;
`;

const StyledButton = styled(Button)`
  flex: 0 0 auto;
  margin-left: 0.5rem;
  white-space: nowrap;
`;

type Props = {
  onCreateClick: () => void;
  dataAdapters?: Array<LookupTableAdapter>;
};

function DataAdapterPicker({ onCreateClick, dataAdapters = [] }: Props) {
  const { t } = useTranslation('lookup_tables');
  const [, { value, touched, error }, { setTouched, setValue }] = useField('data_adapter_id');
  const sortedAdapters = dataAdapters
    .map((adapter: LookupTableAdapter) => ({ value: adapter.id, label: `${adapter.title} (${adapter.name})` }))
    .sort((a, b) => naturalSort(a.label.toLowerCase(), b.label.toLowerCase()));

  const errorMessage = touched ? error : '';

  return (
    <fieldset>
      <Input
        id="data-adapter-select"
        label={t('adapter_form.picker_label')}
        required
        bsStyle={errorMessage ? 'error' : undefined}
        labelClassName="d-block mb-1"
        wrapperClassName="d-block"
        formGroupClassName="mb-3">
        <div style={{ display: 'flex', flexDirection: 'row', alignItems: 'center' }}>
          <StyledSelect
            placeholder={t('adapter_form.picker_placeholder')}
            clearable={false}
            options={sortedAdapters}
            onBlur={() => setTouched(true)}
            onChange={(v) => setValue(v)}
            value={value}
          />
          <StyledButton type="button" aria-label={t('adapter_form.picker_create')} onClick={onCreateClick}>
            {t('adapter_form.picker_create')}
          </StyledButton>
        </div>
        <div className={`mb-1 ${errorMessage ? 'text-danger' : 'text-muted'}`}>
          {errorMessage || t('adapter_form.picker_help')}
        </div>
      </Input>
    </fieldset>
  );
}

export default DataAdapterPicker;
