const SidebarEventDetails = (alertId: string, definitionId: string) => ({
  id: 'alert-event-details',
  title: 'Alert/Event Details',
  componentKey: 'replay-search-sidebar',
  props: { alertId, definitionId },
});

export default SidebarEventDetails;
