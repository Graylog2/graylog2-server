// @flow strict
import { WidgetActions } from 'views/stores/WidgetStore';
import type { FieldActionHandler } from './FieldActionHandler';

const AddToTableActionHandler: FieldActionHandler = ({ field, contexts: { widget } }) => {
  const newFields = [].concat(widget.config.fields, [field]);
  const newConfig = widget.config.toBuilder()
    .fields(newFields)
    .build();
  return WidgetActions.updateConfig(widget.id, newConfig);
};

AddToTableActionHandler.isEnabled = ({ contexts: { widget }, field }): boolean => {
  if (widget.constructor.name === 'MessagesWidget' && widget.config.constructor.name === 'MessagesWidgetConfig') {
    const fields = widget.config.fields || [];
    return !fields.includes(field);
  }
  return false;
};

/* Hide AddToTableHandler in the sidebar */
AddToTableActionHandler.isHidden = ({ contexts: { widget } }): boolean => {
  return !widget;
};

export default AddToTableActionHandler;
