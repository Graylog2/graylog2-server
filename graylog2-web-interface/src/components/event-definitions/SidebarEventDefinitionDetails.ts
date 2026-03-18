import ReplaySearchSidebar from 'components/events/ReplaySearchSidebar/ReplaySearchSidebar';

const SidebarEventDefinitionDetails = (id: string) =>
  ({
    id: 'event-definition-details',
    title: 'Event Definition Details',
    component: ReplaySearchSidebar,
    props: { alertId: undefined, definitionId: id },
  }) as const;
export default SidebarEventDefinitionDetails;
