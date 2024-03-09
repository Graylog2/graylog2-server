import type { AppDispatch } from 'stores/useAppDispatch';
import { addWidget } from 'views/logic/slices/widgetActions';

import EventsWidgetConfig from './EventsWidgetConfig';
import EventsWidget from './EventsWidget';

export const CreateEventsWidget = () => EventsWidget.builder()
  .newId()
  .config(EventsWidgetConfig.createDefault())
  .build();

export default () => async (dispatch: AppDispatch) => dispatch(addWidget(CreateEventsWidget()));
