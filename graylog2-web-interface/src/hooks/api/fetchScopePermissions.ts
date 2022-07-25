import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';

export default function fetchScopePermissions() {
  return fetch('GET', qualifyUrl(ApiRoutes.EntityScopeController.getScope().url));
}
