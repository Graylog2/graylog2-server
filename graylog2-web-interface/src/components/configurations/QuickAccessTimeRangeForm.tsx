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

import { Button, Select } from 'components/bootstrap';
import type { TimeRange } from 'views/logic/queries/Query';
import type { TimeRangeType } from 'views/components/searchbar/date-time-picker/TimeRangeDropdown';
import TabRelativeTimeRange from 'views/components/searchbar/date-time-picker/TabRelativeTimeRange';

const typeOptions: Array<{ value: TimeRangeType, name: string }> = [
  { value: 'relative', name: 'Relative' },
  { value: 'absolute', name: 'Absolute' },
  { value: 'keyword', name: 'Keyword' },
];

const QuickAccessTimeRangeFormItem = ({ timerange }: { timerange: TimeRange}) => {
  const handleOnChange = () => {

  };

  return (
    <div>
      <Select options={typeOptions} value={timerange.type} onChange={handleOnChange} />
      <TabRelativeTimeRange />
    </div>
  );
};

const QuickAccessTimeRangeForm = () => {
  return (
    <div className="form-group">
      <label className="control-label">DDDDDD</label>
      <span className="help-block">DDDDD</span>
      <div className="wrapper" />
      <Button bsSize="xs" onClick={() => {}}>Add</Button>
    </div>
  );
};

export default QuickAccessTimeRangeForm;
