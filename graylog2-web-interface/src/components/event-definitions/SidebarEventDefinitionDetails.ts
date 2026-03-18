const SidebarEventDefinitionDetails = (id: string) =>
  ({
    id: 'event-definition-details',
    title: 'Event Definition Details',
    componentKey: 'replay-search-sidebar',
    props: { alertId: undefined, definitionId: id },
  }) as const;
export default SidebarEventDefinitionDetails;
