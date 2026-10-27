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
import { useEffect, useRef, useState } from 'react';
import { useFormik } from 'formik';
import isEqual from 'lodash/isEqual';

import useLocation from 'routing/useLocation';
import useHistory from 'routing/useHistory';
import UserNotification from 'util/UserNotification';
import extractErrorMessage from 'util/extractErrorMessage';

import type { ConfigurationCategory, ConfigurationFormValues } from './types';
import {
  buildConfigurationFormValues,
  buildConfigurationValuesPatch,
  validateConfigurationFormValues,
} from './configurationFields';
import { useUpdateManagedConfiguration } from './useManagedConfiguration';

type Options = {
  categories: Array<ConfigurationCategory>;
  nodeId: string | null;
  basePath: string;
};

const useDataNodeConfigurationForm = ({ categories, nodeId, basePath }: Options) => {
  const { updateConfiguration } = useUpdateManagedConfiguration(nodeId);
  const { push } = useHistory();
  const { pathname, hash, key: locationKey } = useLocation();
  const lastFocusedSubmit = useRef(0);
  const saveOnlyRequested = useRef(false);
  const [savedRestartNames, setSavedRestartNames] = useState<Array<string>>([]);
  const parameters = categories.flatMap((category) => category.parameters);
  const serverValues = buildConfigurationFormValues(parameters);
  const latestServerSnapshot = useRef({ values: serverValues, hasDeferredRefresh: false });

  const formik = useFormik<ConfigurationFormValues>({
    initialValues: serverValues,
    validateOnChange: false,
    validate: (values) => validateConfigurationFormValues(values, parameters),
    onSubmit: async (draftValues, { resetForm, setStatus }) => {
      setStatus(undefined);
      const saveOnly = saveOnlyRequested.current;
      const changes = buildConfigurationValuesPatch(formik.initialValues, draftValues, parameters);
      const changedRestartNames = parameters
        .filter(({ name, requires_restart }) => requires_restart && Object.hasOwn(changes, name))
        .map(({ name }) => name);

      try {
        const saved = await updateConfiguration(changes);
        latestServerSnapshot.current.hasDeferredRefresh = false;
        setSavedRestartNames((pending) => (saveOnly ? [...new Set([...pending, ...changedRestartNames])] : []));
        resetForm({ values: buildConfigurationFormValues(saved.parameters) });
        UserNotification.success('Configuration saved.');
      } catch (error) {
        const details =
          error instanceof Error || typeof error === 'string' ? extractErrorMessage(error).trim() : undefined;
        setStatus(
          ['Could not save Data Node configuration.', details, 'Your changes have been kept. Please try again.']
            .filter(Boolean)
            .join(' '),
        );
      }
    },
  });
  const { dirty: hasDraftChanges, errors, isSubmitting, isValidating, resetForm, submitCount } = formik;
  const changedValues = buildConfigurationValuesPatch(formik.initialValues, formik.values, parameters);
  const hasUnsavedChanges = Object.keys(changedValues).length > 0;
  const restartParameters = parameters.filter(
    ({ name, requires_restart }) =>
      requires_restart && (Object.hasOwn(changedValues, name) || savedRestartNames.includes(name)),
  );
  const hasUnsavedRestartChanges = restartParameters.some(({ name }) => Object.hasOwn(changedValues, name));

  const saveChangesWithoutRestart = async () => {
    saveOnlyRequested.current = true;
    try {
      await formik.submitForm();
    } finally {
      saveOnlyRequested.current = false;
    }
  };

  useEffect(() => {
    const snapshot = latestServerSnapshot.current;

    if (!isEqual(snapshot.values, serverValues)) {
      snapshot.values = serverValues;
      snapshot.hasDeferredRefresh = true;
    }
    if ((snapshot.hasDeferredRefresh || hasDraftChanges) && !hasUnsavedChanges && !isSubmitting) {
      snapshot.hasDeferredRefresh = false;
      resetForm({ values: snapshot.values });
    }
  }, [hasUnsavedChanges, hasDraftChanges, serverValues, isSubmitting, resetForm]);

  useEffect(() => {
    if (hash) document.getElementById(decodeURIComponent(hash.slice(1)))?.focus();
  }, [hash, locationKey, pathname]);

  useEffect(() => {
    if (submitCount === 0) lastFocusedSubmit.current = 0;
    if (submitCount <= lastFocusedSubmit.current || isSubmitting || isValidating) return;

    const invalidCategory = categories.find((category) => category.parameters.some(({ name }) => errors[name]));
    const invalidParameter = invalidCategory?.parameters.find(({ name }) => errors[name]);

    if (invalidCategory && invalidParameter) {
      const path = `${basePath}/${invalidCategory.id}`;

      if (pathname.replace(/\/$/, '') !== path) {
        push(path);

        return;
      }
      document.getElementById(invalidParameter.name)?.focus();
    }
    lastFocusedSubmit.current = submitCount;
  }, [basePath, categories, errors, isSubmitting, isValidating, pathname, push, submitCount]);

  return { formik, hasUnsavedChanges, restartParameters, hasUnsavedRestartChanges, saveChangesWithoutRestart };
};

export default useDataNodeConfigurationForm;
