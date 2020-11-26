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
import PropTypes from 'prop-types';

import Select from 'views/components/Select';
import Direction from 'views/logic/aggregationbuilder/Direction';

type Props = {
  direction: string | undefined | null,
  disabled: boolean,
  onChange: (newDirection: Direction) => any,
};

const SortDirectionSelect = ({ direction, disabled, onChange }: Props) => (
  <Select isDisabled={disabled}
          isClearable={false}
          isSearchable={false}
          options={[
            { label: 'Ascending', value: 'Ascending' },
            { label: 'Descending', value: 'Descending' },
          ]}
          onChange={({ value }) => onChange(Direction.fromString(value))}
          placeholder={disabled ? 'No sorting selected' : 'Click to select direction'}
          value={(direction && { label: direction, value: direction }) ?? null} />
);

SortDirectionSelect.propTypes = {
  direction: PropTypes.string,
  disabled: PropTypes.bool,
  onChange: PropTypes.func,
};

SortDirectionSelect.defaultProps = {
  direction: undefined,
  disabled: false,
  onChange: () => {},
};

export default SortDirectionSelect;
