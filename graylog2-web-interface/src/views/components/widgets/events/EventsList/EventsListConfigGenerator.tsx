import EventsWidget from 'src/views/logic/widgets/events/EventsWidget';

const EventsListConfigGenerator = ({ config: { filters } }: EventsWidget) => [{
  type: EventsWidget.type,
  filters,
}];

export default EventsListConfigGenerator;
