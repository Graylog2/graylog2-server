// @flow strict
import { WidgetStore, WidgetActions } from 'enterprise/stores/WidgetStore';
import type { ActionContexts } from 'enterprise/logic/ActionContext';
import type { FieldActionHandlerWithContext } from 'enterprise/logic/fieldactions/FieldActionHandler';
import FieldType from 'enterprise/logic/fieldtypes/FieldType';

/* eslint-disable-next-line no-unused-vars */
const RemoveFromAllTablesActionHandler: FieldActionHandlerWithContext = (queryId: string, field: string, type: FieldType, context: ActionContexts) => {
  const widgets = WidgetStore.getInitialState();
  const newWidgets = widgets.map((widget) => {
    if (widget.type.toUpperCase() === 'MESSAGES') {
      const newFields = widget.config.fields.filter(f => (f !== field));
      const newConfig = widget.config.toBuilder()
        .fields(newFields)
        .build();
      return widget.toBuilder().config(newConfig).build();
    }
    return widget;
  });
  return WidgetActions.updateWidgets(newWidgets);
};

export default RemoveFromAllTablesActionHandler;
