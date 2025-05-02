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
import { useCallback, useMemo } from 'react';
import { useQuery } from '@tanstack/react-query';

import { SystemFields } from '@graylog/server-api';

import { HelpBlock, Input } from 'components/bootstrap';
import Select from 'components/common/Select/Select';
import Spinner from 'components/common/Spinner';
import { naturalSortIgnoreCase } from 'util/SortUtils';

type Props = {
  /** ID of the input. */
  id: string;
  /** Label of the field input */
  label?: string;
  /** Specifies if the input should have the input focus or not. */
  autoFocus?: boolean;
  /** Function that is called when the input changes. */
  onChange?: (e: { target: { value: string; name: string } }) => void;
  onBlur?: (e: React.FocusEvent<HTMLInputElement>) => void;
  /** Display an error for the input * */
  error?: string;
  name?: string;
  defaultValue?: string;
};

/**
 * Component that renders an input offering auto-completion for message fields.
 * Fields are loaded from the Graylog server in the background.
 */
const noop = () => {};
const prepareOptions = (data) =>
  data.fields.sort(naturalSortIgnoreCase).map((fieldName) => ({ label: fieldName, value: fieldName }));

const TypeAheadFieldInput = ({
  id,
  autoFocus = false,
  label = undefined,
  onChange = noop,
  onBlur = noop,
  error = undefined,
  name = undefined,
  defaultValue = undefined,
}: Props) => {
  const { data, isInitialLoading } = useQuery(['system', 'fields'], () => SystemFields.fields());
  const options = useMemo(() => (isInitialLoading ? [] : prepareOptions(data)), [data, isInitialLoading]);
  const _onChange = useCallback(
    (fieldName: string) => onChange({ target: { value: fieldName, name } }),
    [name, onChange],
  );

  return isInitialLoading ? (
    <Spinner />
  ) : (
    <Input label={label} error={error}>
      <Select
        inputId={id}
        onChange={_onChange}
        onBlur={onBlur}
        value={defaultValue}
        options={options}
        placeholder="Select Field"
        autoFocus={autoFocus}
      />
      <HelpBlock />
    </Input>
  );
};

export default TypeAheadFieldInput;
