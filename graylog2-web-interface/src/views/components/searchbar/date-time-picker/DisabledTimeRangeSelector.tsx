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

import Input from 'components/bootstrap/Input';

const DisabledTimeRangeSelector = () => (
  <Input id="no-override-timerange-selector"
         type="select"
         disabled
         value="disabled"
         title="There is no override for the timerange currently selected"
         name="no-override">
    <option value="disabled">No Override</option>
  </Input>
);

export default DisabledTimeRangeSelector;
