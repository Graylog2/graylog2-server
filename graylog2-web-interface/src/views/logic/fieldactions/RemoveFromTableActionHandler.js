// @flow strict
import { WidgetActions } from 'views/stores/WidgetStore';
import MessagesWidget from 'views/logic/widgets/MessagesWidget';
import type { FieldActionHandlerCondition, FieldActionHandler } from './FieldActionHandler';

const RemoveFromTableActionHandler: FieldActionHandler = ({ field, contexts: { widget } }) => {
  const newFields = widget.config.fields.filter(f => (f !== field));
  const newConfig = widget.config.toBuilder()
    .fields(newFields)
    .build();
  return WidgetActions.updateConfig(widget.id, newConfig);
};

const isEnabled: FieldActionHandlerCondition = ({ contexts: { widget }, field }) => {
  if (MessagesWidget.isMessagesWidget(widget) && widget.config) {
    const fields = widget.config.fields || [];
    return fields.includes(field);
  }
  return false;
};

/* Hide RemoveFromTableHandler in the sidebar */
const isHidden: FieldActionHandlerCondition = ({ contexts: { widget } }): boolean => !widget;

RemoveFromTableActionHandler.isEnabled = isEnabled;
RemoveFromTableActionHandler.isHidden = isHidden;

export default RemoveFromTableActionHandler;
