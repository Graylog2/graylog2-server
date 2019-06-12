// @flow strict
import { WidgetStore, WidgetActions } from 'views/stores/WidgetStore';
import type { FieldActionHandler } from 'views/logic/fieldactions/FieldActionHandler';
import type { ActionContexts } from 'views/logic/ActionContext';
import FieldType from 'views/logic/fieldtypes/FieldType';

/* eslint-disable-next-line no-unused-vars */
const AddToAllTablesActionHandler: FieldActionHandler = (queryId: string, field: string, type: FieldType, context: ActionContexts) => {
  const widgets = WidgetStore.getInitialState();
  const newWidgets = widgets.map((widget) => {
    if (widget.type.toUpperCase() === 'MESSAGES') {
      const newFields = [].concat(widget.config.fields, [field]);
      const newConfig = widget.config.toBuilder()
        .fields(newFields)
        .build();
      return widget.toBuilder().config(newConfig).build();
    }
    return widget;
  });
  return WidgetActions.updateWidgets(newWidgets);
};

export default AddToAllTablesActionHandler;
