// @flow strict
import { SelectedFieldsActions, SelectedFieldsStore } from 'enterprise/stores/SelectedFieldsStore';
import { WidgetActions } from 'enterprise/stores/WidgetStore';
import type { ActionContexts } from 'enterprise/logic/ActionContext';
import MessagesWidget from '../widgets/MessagesWidget';
import MessagesWidgetConfig from '../widgets/MessagesWidgetConfig';
import type { FieldActionHandlerCondition, FieldActionHandlerWithContext } from './FieldActionHandler';
import FieldType from '../fieldtypes/FieldType';

const RemoveFromTableActionHandler: FieldActionHandlerWithContext = (queryId: string, field: string, type: FieldType, context: ActionContexts) => {
  if (context.widget) {
    const { widget } = context;
    const newFields = widget.config.fields.filter(f => (f !== field));
    const newConfig = widget.config.toBuilder()
      .fields(newFields)
      .build();
    return WidgetActions.updateConfig(widget.id, newConfig);
  } else {
    return SelectedFieldsActions.remove(field);
  }
};

const condition: FieldActionHandlerCondition = ({ context, name }: { context: ActionContexts, name: string }) => {
  if (context.widget) {
    const { widget } = context;
    if (widget instanceof MessagesWidget && widget.config instanceof MessagesWidgetConfig) {
      const fields = widget.config.fields || [];
      return fields.includes(name);
    }
    return false;
  }

  const fields = SelectedFieldsStore.getInitialState();
  return fields.contains(name);
};

RemoveFromTableActionHandler.condition = condition;

export default RemoveFromTableActionHandler;
