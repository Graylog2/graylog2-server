// @flow strict
import { WidgetActions } from 'enterprise/stores/WidgetStore';
import type { FieldActionHandlerWithContext } from './FieldActionHandler';
import MessagesWidget from '../widgets/MessagesWidget';
import MessagesWidgetConfig from '../widgets/MessagesWidgetConfig';
import FieldType from '../fieldtypes/FieldType';
import type { ActionContexts } from '../ActionContext';

const AddToTableActionHandler: FieldActionHandlerWithContext = (queryId: string, field: string, type: FieldType, context: ActionContexts) => {
  const { widget } = context;
  const newFields = [].concat(widget.config.fields, [field]);
  const newConfig = widget.config.toBuilder()
    .fields(newFields)
    .build();
  return WidgetActions.updateConfig(widget.id, newConfig);
};

AddToTableActionHandler.condition = ({ context, name }: { context: ActionContexts, name: string }) => {
  const { widget } = context;
  if (widget instanceof MessagesWidget && widget.config instanceof MessagesWidgetConfig) {
    const fields = widget.config.fields || [];
    return !fields.includes(name);
  }
  return false;
};

/* Hide AddToTableHandler in the sidebar */
AddToTableActionHandler.hide = (context: ActionContexts): boolean => !context.widget;

export default AddToTableActionHandler;
