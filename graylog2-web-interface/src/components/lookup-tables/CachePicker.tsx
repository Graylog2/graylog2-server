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
import PropTypes from 'prop-types';
import { useField } from 'formik';
import type { LookupTableCache } from 'src/logic/lookup-tables/types';

import { defaultCompare as naturalSort } from 'logic/DefaultCompare';
import { Input } from 'components/bootstrap';
import { Select } from 'components/common';

type Props = {
  caches: LookupTableCache[],
}

const CachePicker = ({ caches }: Props) => {
  const [, { value, touched, error }, { setTouched, setValue }] = useField('cache_id');
  const sortedCaches = caches.map((cache) => {
    return { value: cache.id, label: `${cache.title} (${cache.name})` };
  }).sort((a, b) => naturalSort(a.label.toLowerCase(), b.label.toLowerCase()));

  const errorMessage = touched ? error : '';

  return (
    <fieldset>
      <Input id="cache-select"
             label="Cache"
             required
             autoFocus
             bsStyle={errorMessage ? 'error' : undefined}
             help={errorMessage || 'Select an existing cache'}
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

CachePicker.propTypes = {
  caches: PropTypes.array,
};

CachePicker.defaultProps = {
  caches: [],
};

export default CachePicker;
