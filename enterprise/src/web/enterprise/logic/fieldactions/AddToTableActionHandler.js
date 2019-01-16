// @flow strict
import { SelectedFieldsActions } from 'enterprise/stores/SelectedFieldsStore';
import { WidgetActions } from 'enterprise/stores/WidgetStore';
import { ActionContext, WidgetContext } from 'enterprise/logic/ActionContext';

export default (queryId: string, field: string, context: ActionContext) => {
  if (context instanceof WidgetContext) {
    const { widget } = context;
    const newFields = [].concat(widget.config.fields, [field]);
    const newConfig = widget.config.toBuilder()
      .fields(newFields)
      .build();
    WidgetActions.updateConfig(widget.id, newConfig);
  } else {
    SelectedFieldsActions.add(field);
  }
};
