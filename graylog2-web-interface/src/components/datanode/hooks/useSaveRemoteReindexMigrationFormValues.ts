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
import { useState, useEffect } from 'react';

import type { RemoteReindexRequest } from './useRemoteReindexMigrationStatus';

export const DEFAULT_THREADS_COUNT = 4;

export const DEFAULT_INITIAL_VALUES: RemoteReindexRequest = {
  allowlist: '',
  hostname: '',
  user: '',
  password: '',
  synchronous: false,
  indices: [],
  threads: DEFAULT_THREADS_COUNT,
  trust_unknown_certs: false,
};

export const saveFormValues = (values: RemoteReindexRequest) => {
  sessionStorage.setItem('migrateExistingDataForm', JSON.stringify({ ...values, password: '' }));
};

export const getSavedFormValues = () => JSON.parse(sessionStorage.getItem('migrateExistingDataForm') || '{}');

export const removeSavedFormValues = () => {
  sessionStorage.removeItem('migrateExistingDataForm');
};

const useSaveRemoteReindexMigrationFormValues = (skipSavedValues: boolean = false) : {
  initialValues: RemoteReindexRequest,
  saveFormValues: (values: RemoteReindexRequest) => void,
} => {
  const [initialValues, setInitialValues] = useState<RemoteReindexRequest>(DEFAULT_INITIAL_VALUES);

  useEffect(() => {
    if (!skipSavedValues) {
      const savedValues = getSavedFormValues();
      setInitialValues((previousValues) => ({ ...previousValues, ...savedValues }));
    }
  }, [skipSavedValues]);

  return ({
    initialValues,
    saveFormValues,
  });
};

export default useSaveRemoteReindexMigrationFormValues;
