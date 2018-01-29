import { action, observable } from 'mobx';

import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';

class JvmInfoStore {
  constructor() {
    this.state = observable.object({
      isLoading: true,
      error: undefined,
      jvmInfo: undefined,
    }, 'JvmInfoStore state');
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

  getJvmInfo = action(function getJvmInfo() {
    this.state.isLoading = true;

    const url = URLUtils.qualifyUrl(ApiRoutes.SystemApiController.jvm().url);
    return fetch('GET', url)
      .then(
        action((response) => {
          this.state.jvmInfo = response;
        }),
        action((error) => {
          this.state.error = error;
        }),
      )
      .finally(action(() => {
        this.state.isLoading = false;
      }));
  })
}

export default new JvmInfoStore();
