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

type Props = {
  disabled: boolean,
  error: string | undefined,
  fieldName: 'range' | 'from' | 'to',
  onChange: (changeEvent: { target: { name: string, value: string } }) => void,
  unsetRange: boolean,
  value: number | null
}

const RangeValueInput = React.memo(({ fieldName, unsetRange, value, onChange, disabled, error }: Props) => (
  <Input id={`relative-timerange-${fieldName}-value`}
         name={`relative-timerange-${fieldName}-value`}
         disabled={disabled || unsetRange}
         type="number"
         min="1"
         value={value === null ? '' : value}
         className="mousetrap"
         title={`Set the ${fieldName} value`}
         onChange={onChange}
         bsStyle={error ? 'error' : null} />
));

export default RangeValueInput;
