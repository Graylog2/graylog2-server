// @flow strict
import { WidgetActions } from 'views/stores/WidgetStore';
import MessagesWidget from '../widgets/MessagesWidget';
import MessagesWidgetConfig from '../widgets/MessagesWidgetConfig';
import type { ActionContexts } from '../ActionContext';
import type { FieldActionHandler } from './FieldActionHandler';

const AddToTableActionHandler: FieldActionHandler = ({ field, contexts: { widget } }) => {
  const newFields = [].concat(widget.config.fields, [field]);
  const newConfig = widget.config.toBuilder()
    .fields(newFields)
    .build();
  return WidgetActions.updateConfig(widget.id, newConfig);
};

AddToTableActionHandler.isEnabled = ({ contexts: { widget }, field }) => {
  if (widget instanceof MessagesWidget && widget.config instanceof MessagesWidgetConfig) {
    const fields = widget.config.fields || [];
    return !fields.includes(field);
  }
  return false;
};

/* Hide AddToTableHandler in the sidebar */
AddToTableActionHandler.isHidden = (context: ActionContexts): boolean => !context.widget;

export default AddToTableActionHandler;
