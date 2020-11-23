// @flow strict

import AppConfig from '../AppConfig';

type ValueAction = { type: string };

function filterValueActions(
  items: Array<ValueAction>,
  toExclude: Array<string>,
): Array<ValueAction> {
  return items.filter((item) => !toExclude.includes(item.type));
}

export function filterCloudValueActions(
  valueActions: Array<ValueAction>,
  toExclude: Array<string>,
): Array<ValueAction> {
  if (!AppConfig.isCloud()) {
    return valueActions;
  }

  return filterValueActions(valueActions, toExclude);
}

export default filterValueActions;
