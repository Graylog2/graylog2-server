// @flow strict

import AppConfig from '../AppConfig';

function isCloud(): boolean {
  return AppConfig.isCloud();
}

export default isCloud;
