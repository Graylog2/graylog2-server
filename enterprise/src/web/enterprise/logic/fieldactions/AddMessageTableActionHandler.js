import { WidgetActions } from 'enterprise/stores/WidgetStore';
import MessagesWidget from '../widgets/MessagesWidget';
import MessagesWidgetConfig from '../widgets/MessagesWidgetConfig';

export default () => WidgetActions.create(MessagesWidget.builder().newId().config(new MessagesWidgetConfig(['message', 'source'], true)).build());
