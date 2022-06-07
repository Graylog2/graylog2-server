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

interface CachePickerProps {
  name: string,
  caches: any[],
}

const CachePicker: React.FC<CachePickerProps> = ({
  name = 'cache_id',
  caches = [],
}) => {
  const field = useField(name);
  const meta = field[1];
  const helpers = field[2];
  const sortedCaches = caches.map((cache) => {
    return { value: cache.id, label: `${cache.title} (${cache.name})` };
  }).sort((a, b) => naturalSort(a.label.toLowerCase(), b.label.toLowerCase()));

  const errorMsg = meta.touched ? meta.error : '';

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
                onBlur={() => helpers.setTouched(true)}
                onChange={(value) => helpers.setValue(value)}
                value={meta.value} />
      </Input>
    </fieldset>
  );
};

export default CachePicker;
