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
import { Field } from 'formik';
import { useMemo } from 'react';

import Select from 'components/common/Select';
import { Input } from 'components/bootstrap';
import type { IndexSet } from 'stores/indices/IndexSetsStore';

type Props = {
  indexSets: Array<IndexSet>
  help?: string,
}

const IndexSetSelect = ({ indexSets, help = 'Messages that match this stream will be written to the configured index set.' }: Props) => {
  const indexSetOptions = useMemo(
    () => indexSets
      .filter((indexSet) => indexSet.can_be_default)
      .map(({ id, title }) => ({
        value: id,
        label: title,
      })),
    [indexSets],
  );

  return (
    <Field name="index_set_id">
      {({ field: { name, value, onChange, onBlur }, meta: { error, touched } }) => (
        <Input label="Index Set"
               help={help}
               id={name}
               error={(error && touched) ? error : undefined}>
          <Select onBlur={onBlur}
                  onChange={(newValue: number) => onChange({
                    target: { value: newValue, name },
                  })}
                  options={indexSetOptions}
                  inputId={name}
                  placeholder="Select an index set"
                  value={value} />
        </Input>
      )}
    </Field>
  );
};

export default IndexSetSelect;
