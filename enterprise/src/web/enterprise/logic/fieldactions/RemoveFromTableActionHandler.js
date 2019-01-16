// @flow strict
import { SelectedFieldsActions, SelectedFieldsStore } from 'enterprise/stores/SelectedFieldsStore';
import { WidgetActions } from 'enterprise/stores/WidgetStore';
import { ActionContext, WidgetContext } from 'enterprise/logic/ActionContext';

const RemoveFromTableActionHandler = (queryId: string, field: string, context: ActionContext) => {
  if (context instanceof WidgetContext) {
    const { widget } = context;
    const newFields = widget.config.fields.filter(f => (f !== field));
    const newConfig = widget.config.toBuilder()
      .fields(newFields)
      .build();
    WidgetActions.updateConfig(widget.id, newConfig);
  } else {
    SelectedFieldsActions.remove(field);
  }
};

RemoveFromTableActionHandler.condition = ({ context, name }) => {
  if (context instanceof WidgetContext) {
    const { widget } = context;
    return widget.config.fields.includes(name);
  }

  const fields = SelectedFieldsStore.getInitialState();
  return fields.contains(name);
};

export default RemoveFromTableActionHandler;