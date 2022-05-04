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
import { merge } from 'lodash';

import UserNotification from 'util/UserNotification';
import type { SearchBarControl, CombinedSearchBarFormValues } from 'views/types';
import usePluginEntities from 'views/logic/usePluginEntities';

const executeSafely = <T extends () => ReturnType<T>>(fn: T, errorMessage: string, fallbackResult?: ReturnType<T>): ReturnType<T> => {
  try {
    return fn();
  } catch (e) {
    // eslint-disable-next-line no-console
    console.error(e);
    UserNotification.error(`${errorMessage}: ${e}`);
  }

  return fallbackResult;
};

export const usePluggableInitialValues = () => {
  const pluggableSearchBarControls = usePluginEntities('views.components.searchBar') ?? [];
  const initialValuesHandler = pluggableSearchBarControls?.map((pluginFn) => pluginFn()?.useInitialValues).filter((useInitialValues) => !!useInitialValues);

  const initialValues = initialValuesHandler.map((useInitialValues) => executeSafely(
    // eslint-disable-next-line react-hooks/rules-of-hooks
    () => useInitialValues(),
    'An error occurred when collecting initial search bar form values from a plugin',
    {},
  ));

  return merge({}, ...initialValues);
};

export const executePluggableSubmitHandler = (values: CombinedSearchBarFormValues, pluggableSearchBarControls: Array<() => SearchBarControl>) => {
  const pluginSubmitHandler = pluggableSearchBarControls?.map((pluginFn) => pluginFn()?.onSubmit).filter((pluginData) => !!pluginData);

  const executableSubmitHandler = pluginSubmitHandler.map((onSubmit) => new Promise((resolve) => {
    resolve(onSubmit(values));
  }).catch((e) => {
    // eslint-disable-next-line no-console
    console.error(e);
    UserNotification.error(`An error occurred when executing a submit handler from a plugin: ${e}`);
  }));

  return Promise.all(executableSubmitHandler);
};

export const pluggableValidationPayload = (values: CombinedSearchBarFormValues, pluggableSearchBarControls: Array<() => SearchBarControl> = []) => {
  const validationPayloadHandler = pluggableSearchBarControls.map((pluginFn) => pluginFn()?.validationPayload).filter((validationPayloadFn) => !!validationPayloadFn);
  const validationPayload: Array<{ [key: string ]: any }> = validationPayloadHandler.map((validationPayloadFn) => executeSafely(
    () => validationPayloadFn(values),
    'An error occurred when preparing search bar validation for a plugin',
    {},
  ));

  return merge({}, ...validationPayload);
};

export const validatePluggableValues = async (values: CombinedSearchBarFormValues, pluggableSearchBarControls: Array<() => SearchBarControl> = []) => {
  const validationHandler = pluggableSearchBarControls.map((pluginFn) => pluginFn()?.onValidate).filter((onValidate) => !!onValidate);

  const errors = validationHandler.map((onValidate) => executeSafely(
    () => onValidate(values),
    'An error occurred when validating search bar values from a plugin',
    {},
  ));

  return merge({}, ...errors);
};
