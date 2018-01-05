import { observable } from 'mobx';

import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';

class SystemMobXStore {
  constructor() {
    this.state = observable({
      isLoading: true,
      error: undefined,
      systemInfo: undefined,
    });
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
        (response) => {
          this.state.systemInfo = response;
        },
        (error) => {
          this.state.error = error;
        },
      )
      .finally(() => {
        this.state.isLoading = false;
      });
  }
}

export default new SystemMobXStore();
