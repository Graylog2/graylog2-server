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
import { useField } from 'formik';
import type { LookupTableAdapter } from 'src/logic/lookup-tables/types';

import { Input } from 'components/bootstrap';
import { Select } from 'components/common';

type Props = {
  dataAdapters: LookupTableAdapter[],
}

type OptionType = {
  label: string,
  value: string,
};

const DataAdapterPicker = ({ dataAdapters }: Props) => {
  const sortedAdapters = React.useMemo(() => (
    dataAdapters.map((adapter: LookupTableAdapter) => (
      { value: adapter.id, label: `${adapter.title} (${adapter.name})` }
    )).sort((a: OptionType, b: OptionType) => {
      if (a.label.toLowerCase() > b.label.toLowerCase()) return 1;
      if (a.label.toLowerCase() < b.label.toLowerCase()) return -1;

      return 0;
    })
  ), [dataAdapters]);

  const [, { value, touched, error }, { setTouched, setValue }] = useField('data_adapter_id');
  const errorMessage = touched ? error : '';

  return (
    <fieldset>
      <Input id="data-adapter-select"
             label="Data Adapter"
             required
             autoFocus
             bsStyle={errorMessage ? 'error' : undefined}
             help={errorMessage || 'Select an existing data adapter'}
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9">
        <Select placeholder="Select a data adapter"
                clearable={false}
                options={sortedAdapters}
                matchProp="label"
                onBlur={() => setTouched(true)}
                onChange={setValue}
                value={value} />
      </Input>
    </fieldset>
  );
};

export default DataAdapterPicker;
