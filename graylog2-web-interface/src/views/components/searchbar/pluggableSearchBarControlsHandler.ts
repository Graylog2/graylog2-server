import { merge } from 'lodash';

import type { SearchBarControl } from 'views/types';
import usePluginEntities from 'views/logic/usePluginEntities';
import type { TimeRange, NoTimeRangeOverride } from 'views/logic/queries/Query';

type FormValues = {
  timerange: TimeRange | NoTimeRangeOverride,
  streams?: Array<string>,
  queryString: string,
}

export const usePluggableInitialValues = () => {
  const pluggableSearchBarControls = usePluginEntities('views.components.searchBar') ?? [];
  const initialValuesHandler = pluggableSearchBarControls?.map((pluginFn) => pluginFn()?.useInitialValues).filter((useInitialValues) => !!useInitialValues);
  // eslint-disable-next-line react-hooks/rules-of-hooks
  const initialValues = initialValuesHandler.map((useInitialValues) => useInitialValues());

  return merge({}, ...initialValues);
};

export const executePluggableSubmitHandler = async (values: FormValues, pluggableSearchBarControls: Array<() => SearchBarControl>) => {
  const pluginSubmitHandler = pluggableSearchBarControls?.map((pluginFn) => pluginFn()?.onSubmit).filter((pluginData) => !!pluginData);

  if (pluginSubmitHandler?.length) {
    await pluginSubmitHandler.forEach((onSubmit) => onSubmit(values));
  }
};

export const pluggableValidationPayload = (values: FormValues, pluggableSearchBarControls: Array<() => SearchBarControl> = []) => {
  const validationPayloadHandler = pluggableSearchBarControls.map((pluginFn) => pluginFn()?.validationPayload).filter((validationPayloadFn) => !!validationPayloadFn);
  const validationPayload: Array<{ [key: string ]: any }> = validationPayloadHandler.map((validationPayloadFn) => validationPayloadFn(values));

  return merge({}, ...validationPayload);
};

export const validatePluggableValues = async (values: FormValues, pluggableSearchBarControls: Array<() => SearchBarControl> = []) => {
  const validationHandler = pluggableSearchBarControls.map((pluginFn) => pluginFn()?.onValidate).filter((onValidate) => !!onValidate);
  const executableValidationHandler = validationHandler.map((onValidate) => new Promise((resolve) => {
    resolve(onValidate(values));
  }));
  const errors = await Promise.all(executableValidationHandler);

  return merge({}, ...errors);
};
