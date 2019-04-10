// @flow strict
import { SelectedFieldsActions, SelectedFieldsStore } from 'enterprise/stores/SelectedFieldsStore';
import { WidgetActions } from 'enterprise/stores/WidgetStore';
import type { FieldActionHandlerWithContext } from './FieldActionHandler';
import MessagesWidget from '../widgets/MessagesWidget';
import MessagesWidgetConfig from '../widgets/MessagesWidgetConfig';
import FieldType from '../fieldtypes/FieldType';
import type { ActionContexts } from '../ActionContext';

const AddToTableActionHandler: FieldActionHandlerWithContext = (queryId: string, field: string, type: FieldType, context: ActionContexts) => {
  if (context.widget) {
    const { widget } = context;
    const newFields = [].concat(widget.config.fields, [field]);
    const newConfig = widget.config.toBuilder()
      .fields(newFields)
      .build();
    return WidgetActions.updateConfig(widget.id, newConfig);
  }
  return SelectedFieldsActions.add(field);
};

AddToTableActionHandler.condition = ({ context, name }: { context: ActionContexts, name: string }) => {
  if (context.widget) {
    const { widget } = context;
    if (widget instanceof MessagesWidget && widget.config instanceof MessagesWidgetConfig) {
      const fields = widget.config.fields || [];
      return !fields.includes(name);
    }
    return false;
  }

  const fields = SelectedFieldsStore.getInitialState();
  return !fields.contains(name);
};

export default AddToTableActionHandler;
