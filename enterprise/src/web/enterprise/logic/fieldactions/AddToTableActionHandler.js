// @flow strict
import { SelectedFieldsActions, SelectedFieldsStore } from 'enterprise/stores/SelectedFieldsStore';
import { WidgetActions } from 'enterprise/stores/WidgetStore';
import { ActionContext, WidgetContext } from 'enterprise/logic/ActionContext';
import type { FieldActionHandlerWithContext } from './FieldActionHandler';
import MessagesWidget from '../widgets/MessagesWidget';
import MessagesWidgetConfig from '../widgets/MessagesWidgetConfig';
import FieldType from '../fieldtypes/FieldType';

const AddToTableActionHandler: FieldActionHandlerWithContext = (queryId: string, field: string, type: FieldType, context: ActionContext) => {
  if (context instanceof WidgetContext) {
    const { widget } = context;
    const newFields = [].concat(widget.config.fields, [field]);
    const newConfig = widget.config.toBuilder()
      .fields(newFields)
      .build();
    return WidgetActions.updateConfig(widget.id, newConfig);
  } else {
    return SelectedFieldsActions.add(field);
  }
};

AddToTableActionHandler.condition = ({ context, name }: { context: ActionContext, name: string }) => {
  if (context instanceof WidgetContext) {
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
