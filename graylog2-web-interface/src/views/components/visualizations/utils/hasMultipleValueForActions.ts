import type { ActionContexts } from 'views/types';
import { multipleValuesActionsSupportedVisualizations } from 'views/Constants';

const hasMultipleValueForActions = (actionContexts: Partial<ActionContexts>) =>
  actionContexts?.valuePath?.length &&
  multipleValuesActionsSupportedVisualizations.includes(actionContexts.widget.config.visualization);

export default hasMultipleValueForActions;
