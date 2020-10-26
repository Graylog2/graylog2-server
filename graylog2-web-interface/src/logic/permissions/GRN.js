// @flow strict
// eslint-disable-next-line import/prefer-default-export
import Routes from 'routing/Routes';

const _convertEmptyString = (value: string) => (value === '' ? undefined : value);

export const createGRN = (type: string, id: string) => `grn::::${type}:${id}`;

export const getValuesFromGRN = (grn: string) => {
  const grnValues = grn.split(':');
  const [resourceNameType, cluster, tenent, scope, type, id] = grnValues.map(_convertEmptyString);

  return { resourceNameType, cluster, tenent, scope, type, id };
};

export const getShowRouteFromGRN = (grn: string) => {
  const { id, type } = getValuesFromGRN(grn);

  switch (type) {
    case 'user':
      return Routes.SYSTEM.USERS.show(id);
    case 'team':
      return Routes.getPluginRoute('SYSTEM_TEAMS_TEAMID')(id);
    case 'dashboard':
      return Routes.dashboard_show(id);
    case 'event_definition':
      return Routes.ALERTS.DEFINITIONS.edit(id);
    case 'notification':
      return Routes.ALERTS.NOTIFICATIONS.edit(id);
    case 'search':
      return Routes.getPluginRoute('SEARCH_VIEWID')(id);
    case 'stream':
      return Routes.stream_search(id);
    default:
      throw new Error(`Can't find route for grn ${grn} of type: ${type ?? '(undefined)'}`);
  }
};
