// @flow strict
import { WidgetActions } from 'views/stores/WidgetStore';
import type { ActionContexts } from 'views/logic/ActionContext';
import MessagesWidget from '../widgets/MessagesWidget';
import MessagesWidgetConfig from '../widgets/MessagesWidgetConfig';
import type {
  FieldActionHandlerCondition,
  FieldActionHandler,
} from './FieldActionHandler';

const RemoveFromTableActionHandler = ({ field, contexts: { widget } }) => {
  const newFields = widget.config.fields.filter(f => (f !== field));
  const newConfig = widget.config.toBuilder()
    .fields(newFields)
    .build();
  return WidgetActions.updateConfig(widget.id, newConfig);
};

const isEnabled: FieldActionHandlerCondition = ({ contexts: { widget }, field }) => {
  if (widget instanceof MessagesWidget && widget.config instanceof MessagesWidgetConfig) {
    const fields = widget.config.fields || [];
    return fields.includes(field);
  }
  return false;
};

RemoveFromTableActionHandler.isEnabled = isEnabled;

/* Hide RemoveFromTableHandler in the sidebar */
RemoveFromTableActionHandler.isHidden = (context: ActionContexts): boolean => !context.widget;

export default (RemoveFromTableActionHandler: FieldActionHandler);
