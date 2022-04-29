import { merge } from 'lodash';

import UserNotification from 'util/UserNotification';
import type { SearchBarControl } from 'views/types';
import usePluginEntities from 'views/logic/usePluginEntities';
import type { TimeRange, NoTimeRangeOverride } from 'views/logic/queries/Query';

type FormValues = {
  timerange: TimeRange | NoTimeRangeOverride,
  streams?: Array<string>,
  queryString: string,
}

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

export const executePluggableSubmitHandler = (values: FormValues, pluggableSearchBarControls: Array<() => SearchBarControl>) => {
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

export const pluggableValidationPayload = (values: FormValues, pluggableSearchBarControls: Array<() => SearchBarControl> = []) => {
  const validationPayloadHandler = pluggableSearchBarControls.map((pluginFn) => pluginFn()?.validationPayload).filter((validationPayloadFn) => !!validationPayloadFn);
  const validationPayload: Array<{ [key: string ]: any }> = validationPayloadHandler.map((validationPayloadFn) => executeSafely(
    () => validationPayloadFn(values),
    'An error occurred when preparing search bar validation for a plugin',
    {},
  ));

  return merge({}, ...validationPayload);
};

export const validatePluggableValues = async (values: FormValues, pluggableSearchBarControls: Array<() => SearchBarControl> = []) => {
  const validationHandler = pluggableSearchBarControls.map((pluginFn) => pluginFn()?.onValidate).filter((onValidate) => !!onValidate);

  const errors = validationHandler.map((onValidate) => executeSafely(
    () => onValidate(values),
    'An error occurred when validating search bar values from a plugin',
    {},
  ));

  return merge({}, ...errors);
};
