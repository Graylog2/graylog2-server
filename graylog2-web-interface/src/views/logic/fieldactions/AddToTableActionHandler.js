// @flow strict
import { WidgetActions } from 'views/stores/WidgetStore';
import type { ActionContexts } from 'views/logic/ActionContext';
import type { FieldName } from 'views/logic/fieldtypes/FieldType';
import type { FieldActionHandler } from 'views/logic/fieldactions/FieldActionHandler';

const AddToTableActionHandler: FieldActionHandler = ({ field, contexts: { widget } }) => {
  const newFields = [].concat(widget.config.fields, [field]);
  const newConfig = widget.config.toBuilder()
    .fields(newFields)
    .build();
  return WidgetActions.updateConfig(widget.id, newConfig);
};

AddToTableActionHandler.isEnabled = (
  { contexts: { widget }, field }: { contexts: ActionContexts, field: FieldName},
): boolean => {
  if (widget.type === 'messages') {
    const fields = widget.config.fields || [];
    return !fields.includes(field);
  }
  return false;
};

/* Hide AddToTableHandler in the sidebar */
AddToTableActionHandler.isHidden = (
  { contexts: { widget } }: { contexts: ActionContexts },
): boolean => !widget;

export default AddToTableActionHandler;
