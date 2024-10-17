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

type OptionsProps = {
  value: string;
  label: string;
};

function Options({
  value,
  label,
}: OptionsProps) {
  return (
    <option value={value} key={value}>{label}</option>
  );
}

const renderOptions = (options = [], label = 'Choose One', loading = false) => {
  if (loading) {
    return Options({ value: '', label: 'Loading...' });
  }

  return (
    <>
      <option value="">{label}</option>
      {options.map((option) => Options({ value: option.value, label: option.label }))}
    </>
  );
};

export default Options;

export { renderOptions };
