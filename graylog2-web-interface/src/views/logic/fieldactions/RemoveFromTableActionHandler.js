// @flow strict
import { WidgetActions } from 'views/stores/WidgetStore';
import type { ActionContexts } from 'views/logic/ActionContext';
import type { FieldName } from 'views/logic/fieldtypes/FieldType';
import type {
  FieldActionHandlerCondition,
  FieldActionHandler,
} from './FieldActionHandler';

const RemoveFromTableActionHandler = ({ field, contexts: { widget } }) => {
  const newFields = widget.config.fields.filter(f => (f !== field));
  const newConfig = widget.config.toBuilder()
    .fields(newFields)
    .build();
  return WidgetActions.updateConfig(widget.id, newConfig);
};

const isEnabled: FieldActionHandlerCondition = (
  { contexts: { widget }, field }: { contexts: ActionContexts, field: FieldName},
) => {
  if (widget.type === 'messages') {
    const fields = widget.config.fields || [];
    return fields.includes(field);
  }
  return false;
};

RemoveFromTableActionHandler.isEnabled = isEnabled;

/* Hide RemoveFromTableHandler in the sidebar */
RemoveFromTableActionHandler.isHidden = (
  { contexts: { widget } }: { contexts: ActionContexts },
): boolean => !widget;

export default (RemoveFromTableActionHandler: FieldActionHandler);
