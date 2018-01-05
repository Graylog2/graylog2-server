import { observable } from 'mobx';

import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';

class JvmInfoStore {
  constructor() {
    this.state = observable({
      isLoading: true,
      error: undefined,
      jvmInfo: undefined,
    });
  }

  get jvmInfo() {
    return this.state.jvmInfo;
  }

  get isLoading() {
    return this.state.isLoading;
  }

  get error() {
    return this.state.error;
  }

  getJvmInfo() {
    this.state.isLoading = true;

    const url = URLUtils.qualifyUrl(ApiRoutes.SystemApiController.jvm().url);
    return fetch('GET', url)
      .then(
        (response) => {
          this.state.jvmInfo = response;
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

export default JvmInfoStore;
