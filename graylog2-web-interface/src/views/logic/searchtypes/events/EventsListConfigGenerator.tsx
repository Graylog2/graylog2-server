import EventsWidget from 'views/logic/widgets/events/EventsWidget';

import EventsListSortConfig from './EventsListSortConfig';

const EventsListConfigGenerator = ({ config: { filters, sort } }: EventsWidget) => [{
  sort: new EventsListSortConfig(sort.field, sort.direction),
  type: EventsWidget.type,
  attributes: filters,
  page: 1,
  per_page: 10,
}];

export default EventsListConfigGenerator;
