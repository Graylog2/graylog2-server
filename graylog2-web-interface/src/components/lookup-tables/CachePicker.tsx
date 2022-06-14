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
import { useField } from 'formik';
import naturalSort from 'javascript-natural-sort';

import { Input } from 'components/bootstrap';
import { Select } from 'components/common';

type Props = {
  name: string,
  caches: any[],
}

const CachePicker = ({
  name = 'cache_id',
  caches = [],
}: Props) => {
  const [, { value, touched, error }, { setTouched, setValue }] = useField(name);
  const sortedCaches = caches.map((cache) => {
    return { value: cache.id, label: `${cache.title} (${cache.name})` };
  }).sort((a, b) => naturalSort(a.label.toLowerCase(), b.label.toLowerCase()));

  const errorMsg = touched ? error : '';

  return (
    <fieldset>
      <Input id="cache-select"
             label="Cache"
             required
             autoFocus
             bsStyle={errorMsg ? 'error' : undefined}
             help={errorMsg || 'Select an existing cache'}
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9">
        <Select placeholder="Select a cache"
                clearable={false}
                options={sortedCaches}
                matchProp="label"
                onBlur={() => setTouched(true)}
                onChange={setValue}
                value={value} />
      </Input>
    </fieldset>
  );
};
CachePicker.defaultProps = {
   name = 'cache_id',
  caches = [],
}
export default CachePicker;
