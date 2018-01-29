import { action, observable } from 'mobx';

import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';

class SystemInfoStore {
  constructor() {
    this.state = observable.object({
      isLoading: true,
      error: undefined,
      systemInfo: undefined,
    }, 'SystemInfoStore state');
    this.getSystemInfo();
  }

  get systemInfo() {
    return this.state.systemInfo;
  }

  get isLoading() {
    return this.state.isLoading;
  }

  get error() {
    return this.state.error;
  }

  getSystemInfo() {
    this.state.isLoading = true;

    const url = URLUtils.qualifyUrl(ApiRoutes.SystemApiController.info().url);
    return fetch('GET', url)
      .then(
        action((response) => {
          this.state.systemInfo = response;
        }),
        action((error) => {
          this.state.error = error;
        }),
      )
      .finally(action(() => {
        this.state.isLoading = false;
      }));
  }
}

export default new SystemInfoStore();
