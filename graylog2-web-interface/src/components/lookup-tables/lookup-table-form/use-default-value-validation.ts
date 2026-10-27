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
import { useEffect, useRef } from 'react';
import { useFormikContext } from 'formik';

import { useValidateLookupTable } from 'components/lookup-tables/hooks/useLookupTablesAPI';
import type { LookupTableType } from 'components/lookup-tables/lookup-table-form';

const DEFAULT_VALUE_FIELDS = ['default_single_value', 'default_multi_value'] as const;

export type DefaultValueField = (typeof DEFAULT_VALUE_FIELDS)[number];
export type DefaultValueErrors = Partial<Record<DefaultValueField, string>>;

// Validated by the server, so the form accepts exactly what the table can load. The errors live in Formik's
// status because the form's validate function replaces `errors` on every change and would drop them.
const useDefaultValueValidation = () => {
  const { values, status, setStatus } = useFormikContext<LookupTableType>();
  const { validateLookupTable } = useValidateLookupTable();
  const latestRequest = useRef(0);

  // Steps unmount when the wizard moves on, so their pending responses must not overwrite a later step's result.
  useEffect(
    () => () => {
      latestRequest.current += 1;
    },
    [],
  );

  const defaultValueErrors: DefaultValueErrors = status?.defaultValueErrors ?? {};

  const validateDefaultValues = (table: LookupTableType = values) => {
    latestRequest.current += 1;
    const request = latestRequest.current;

    return validateLookupTable(table)
      .then(({ errors }) => {
        if (request !== latestRequest.current) return;

        setStatus({
          defaultValueErrors: Object.fromEntries(
            DEFAULT_VALUE_FIELDS.filter((field) => errors[field]?.length).map((field) => [field, errors[field][0]]),
          ),
        });
      })
      .catch(() => {
        // Already reported by the hook; the server still rejects invalid defaults on save.
      });
  };

  const clearDefaultValueError = (field: DefaultValueField) => {
    // Drop in-flight responses too: they describe a value that is no longer there.
    latestRequest.current += 1;

    if (defaultValueErrors[field]) {
      setStatus({ defaultValueErrors: { ...defaultValueErrors, [field]: undefined } });
    }
  };

  const hasDefaultValueErrors = Object.values(defaultValueErrors).some(Boolean);

  return { defaultValueErrors, hasDefaultValueErrors, validateDefaultValues, clearDefaultValueError };
};

export default useDefaultValueValidation;
