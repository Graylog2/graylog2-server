// eslint-disable-next-line import/prefer-default-export
import Direction from 'views/logic/aggregationbuilder/Direction';

export const PAGINATION = {
  INITIAL_PAGE: 1,
  PER_PAGE: 10,
};
export const SORT_DIRECTION_OPTIONS = [Direction.Ascending.direction, Direction.Descending.direction];

export const EVENT_ATTRIBUTES = {
  assigned_to: { title: 'Assigned To', sortable: true, filterable: true },
  created_at: { title: 'Started At', sortable: true, filterable: true },
  name: { title: 'Name', sortable: true, filterable: true },
  priority: { title: 'Priority', sortable: true, filterable: true },
  status: { title: 'Status', sortable: true, filterable: true },
  updated_at: { title: 'Last Update', sortable: true, filterable: true },
};
