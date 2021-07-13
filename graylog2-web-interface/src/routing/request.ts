import Qs from 'qs';

import { Builder } from 'logic/rest/FetchProvider';
import * as URLUtils from 'util/URLUtils';

const request = (method: string, path: string, body: any, query: { [key: string]: string | number | boolean }, headers: { [key: string]: any }) => {
  const pathWithQueryParameters = Object.entries(query).length > 0 ? `${path}?${Qs.stringify(query)}` : path;
  const builder = new Builder(method, URLUtils.qualifyUrl(pathWithQueryParameters))
    .setHeader('X-Graylog-No-Session-Extension', 'true');

  const builderWithHeaders = Object.entries(headers)
    .reduce((prev, [key, value]) => builder.setHeader(key, value), builder);

  return builderWithHeaders.build();
};

export default request;
