// @flow strict
import { WidgetActions } from 'views/stores/WidgetStore';
import type { ActionContexts } from 'views/logic/ActionContext';
import MessagesWidget from '../widgets/MessagesWidget';
import MessagesWidgetConfig from '../widgets/MessagesWidgetConfig';
import type {
  FieldActionHandlerCondition,
  FieldActionHandlerConditionProps,
  FieldActionHandler,
} from './FieldActionHandler';
import FieldType from '../fieldtypes/FieldType';

const RemoveFromTableActionHandler = (queryId: string, field: string, type: FieldType, context: ActionContexts) => {
  const { widget } = context;
  const newFields = widget.config.fields.filter(f => (f !== field));
  const newConfig = widget.config.toBuilder()
    .fields(newFields)
    .build();
  return WidgetActions.updateConfig(widget.id, newConfig);
};

const condition: FieldActionHandlerCondition = ({ context, name }: FieldActionHandlerConditionProps) => {
  const { widget } = context;
  if (widget instanceof MessagesWidget && widget.config instanceof MessagesWidgetConfig) {
    const fields = widget.config.fields || [];
    return fields.includes(name);
  }
  return false;
};

RemoveFromTableActionHandler.condition = condition;

/* Hide RemoveFromTableHandler in the sidebar */
RemoveFromTableActionHandler.hide = (context: ActionContexts): boolean => !context.widget;

export default (RemoveFromTableActionHandler: FieldActionHandler);
