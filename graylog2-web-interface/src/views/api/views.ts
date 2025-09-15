import type View from 'views/logic/views/View';
import type { ViewJson } from 'views/logic/views/View';
import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import type { EntitySharePayload } from 'actions/permissions/EntityShareActions';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';
import UserNotification from 'util/UserNotification';

const viewsUrl = qualifyUrl('/views');
const viewsIdUrl = (id) => qualifyUrl(`/views/${id}`);

export const getView = (viewId: string): Promise<ViewJson> => fetch('GET', viewsIdUrl(viewId));
export const deleteView = (view: View): Promise<void> =>
  fetch('DELETE', viewsIdUrl(view.id)).catch((error) => {
    UserNotification.error(`Deleting view ${view.title} failed with status: ${error}`, 'Could not delete view');
  });
export const createView = (view: View, entityShare?: EntitySharePayload, clonedFrom?: string): Promise<View> => {
  const url = clonedFrom ? viewsIdUrl(clonedFrom) : viewsUrl;
  const promise = fetch('POST', url, JSON.stringify({ entity: view.toJSON(), share_request: entityShare }));

  return promise.then((response) => {
    CurrentUserStore.reload();

    return response;
  });
};

export const updateView = (view: View, entityShare?: EntitySharePayload): Promise<View> =>
  fetch('PUT', viewsIdUrl(view.id), JSON.stringify({ entity: view.toJSON(), share_request: entityShare }));
