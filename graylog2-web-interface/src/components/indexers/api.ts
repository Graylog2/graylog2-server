import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import * as URLUtils from 'util/URLUtils';

export const getIndexerClusterHealth = () => {
  const url = URLUtils.qualifyUrl(ApiRoutes.IndexerClusterApiController.health().url);

  return fetch('GET', url);
};

export const getIndexerClusterName = () => {
  const url = URLUtils.qualifyUrl(ApiRoutes.IndexerClusterApiController.name().url);

  return fetch('GET', url);
};
