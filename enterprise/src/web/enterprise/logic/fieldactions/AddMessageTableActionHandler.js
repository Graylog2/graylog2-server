import { WidgetActions } from 'enterprise/stores/WidgetStore';
import { DEFAULT_MESSAGE_FIELDS } from 'enterprise/Constants';

import MessagesWidget from '../widgets/MessagesWidget';
import MessagesWidgetConfig from '../widgets/MessagesWidgetConfig';

export default () => WidgetActions.create(MessagesWidget.builder().newId()
  .config(MessagesWidgetConfig.builder().fields(DEFAULT_MESSAGE_FIELDS).showMessageRow(true).build()).build());
