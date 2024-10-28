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
import { useMemo, forwardRef } from 'react';
import type { FormikErrors } from 'formik';

import type { Editor, AutoCompleter, Command } from 'views/components/searchbar/queryinput/ace-types';
import AsyncQueryInput from 'views/components/searchbar/queryinput/AsyncQueryInput';
import type { TimeRange, NoTimeRangeOverride } from 'views/logic/queries/Query';
import type View from 'views/logic/views/View';
import useUserDateTime from 'hooks/useUserDateTime';
import usePluginEntities from 'hooks/usePluginEntities';
import useFieldTypes from 'views/logic/fieldtypes/useFieldTypes';
import { isNoTimeRangeOverride } from 'views/typeGuards/timeRange';
import { DEFAULT_TIMERANGE } from 'views/Constants';
import type { Completer, FieldTypes } from 'views/components/searchbar/SearchBarAutocompletions';
import type { QueryValidationState } from 'views/components/searchbar/queryvalidation/types';

import SearchBarAutoCompletions from './SearchBarAutocompletions';

const useCompleter = ({ streams, timeRange, completerFactory, view }: Pick<Props, 'streams' | 'timeRange' | 'completerFactory' | 'view'>) => {
  const { userTimezone } = useUserDateTime();
  const completers = usePluginEntities('views.completers');
  const { data: queryFields } = useFieldTypes(streams, isNoTimeRangeOverride(timeRange) ? DEFAULT_TIMERANGE : timeRange);
  const { data: allFields } = useFieldTypes([], DEFAULT_TIMERANGE);
  const fieldTypes = useMemo(() => {
    const queryFieldsByName = Object.fromEntries((queryFields ?? []).map((field) => [field.name, field]));
    const allFieldsByName = Object.fromEntries((allFields ?? []).map((field) => [field.name, field]));

    return { all: allFieldsByName, query: queryFieldsByName };
  }, [allFields, queryFields]);

  return useMemo(() => completerFactory?.(completers ?? [], timeRange, streams, fieldTypes, userTimezone, view),
    [completerFactory, completers, timeRange, streams, fieldTypes, userTimezone, view]);
};

type Props = {
  commands?: Array<Command>,
  completerFactory?: (
    completers: Array<Completer>,
    timeRange: TimeRange | NoTimeRangeOverride | undefined,
    streams: Array<string>,
    fieldTypes: FieldTypes,
    userTimezone: string,
    view: View | undefined,
  ) => AutoCompleter,
  disableExecution?: boolean,
  error?: QueryValidationState,
  inputId?: string
  isValidating: boolean,
  name: string,
  onBlur?: (query: string) => void,
  onChange: (changeEvent: { target: { value: string, name: string } }) => Promise<string>,
  onExecute: (query: string) => void,
  placeholder?: string,
  streams?: Array<string> | undefined,
  timeRange?: TimeRange | NoTimeRangeOverride | undefined,
  validate: () => Promise<FormikErrors<{}>>,
  value: string,
  view?: View
  warning?: QueryValidationState,
}

const defaultCompleterFactory = (...args: ConstructorParameters<typeof SearchBarAutoCompletions>) => new SearchBarAutoCompletions(...args);

const ViewsQueryInput = forwardRef<Editor, Props>(({
  value, timeRange, streams, name, onChange, error,
  isValidating, disableExecution, validate, onExecute,
  commands, view, warning, placeholder, onBlur, inputId,
  completerFactory = defaultCompleterFactory,
}, ref) => {
  const completer = useCompleter({ streams, timeRange, completerFactory, view });

  return (
    <AsyncQueryInput value={value}
                     onBlur={onBlur}
                     inputId={inputId}
                     completer={completer}
                     ref={ref}
                     name={name}
                     onChange={onChange}
                     placeholder={placeholder}
                     error={error}
                     isValidating={isValidating}
                     warning={warning}
                     disableExecution={disableExecution}
                     validate={validate}
                     onExecute={onExecute}
                     commands={commands} />
  );
});

export default ViewsQueryInput;
