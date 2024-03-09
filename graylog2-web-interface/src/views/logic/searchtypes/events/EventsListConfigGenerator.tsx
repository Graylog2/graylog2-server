import EventsWidget from 'views/logic/widgets/events/EventsWidget';

import EventsListSortConfig from './EventsListSortConfig';

const EventsListConfigGenerator = ({ config: { filters, sort } }: EventsWidget) => [{
  sort: new EventsListSortConfig(sort.field, sort.direction),
  type: EventsWidget.type,
  filters,
}];

export default EventsListConfigGenerator;
