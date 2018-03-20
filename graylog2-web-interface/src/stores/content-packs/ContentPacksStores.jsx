import Reflux from 'reflux';

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import ActionsProvider from 'injection/ActionsProvider';

const ContentPacksActions = ActionsProvider.getActions('ContentPacks');

const ContentPacksStores = Reflux.createStore({
  listenables: [ContentPacksActions],

  init() {
    this.contentPack = {
      versions: {
        '1.0': {
          id: '1', title: 'UFW Grok Patterns', summary: 'Grok Patterns to extract informations from UFW logfiles',
          version: '1.0', states: ['installed', 'edited'], vendor: 'graylog.org <info@graylog.org>',
          description: this.readme,
          url: "https://github.com/graylog2/graylog2-server",
          constraints: [
            {type: "Server", name: "graylog", version: "3.0", fullfilled: true},
          ],
        },
        '2.0': {
          id: '1', title: 'UFW Grok Patterns', summary: 'Grok Patterns to extract informations from UFW logfiles',
          version: '2.0', states: ['installed', 'edited'], vendor: 'graylog.org <info@graylog.org>',
          description: this.readme,
          url: "https://github.com/graylog2/graylog2-server",
          constraints: [
            {type: "Server", name: "graylog", version: "3.0", fullfilled: true},
            {type: "Plugin", name: "IntelThreadPlugin", version: "1.0", fullfilled: false},
          ],
        },
      },
      installations: [
        { comment: "Ubuntu Foo", version: '1.0', id: '23434' },
        { comment: "Fedora Foo", version: '2.0', id: '23435' },
      ],
    };
  },

  get(contentPackId) {
    const promise = new Promise((resolve) => {
      setTimeout(() => {
        resolve(this.contentPack);
      }, 300);
    });
    return promise;
  },

  list() {
    const url = URLUtils.qualifyUrl(ApiRoutes.ContentPacksController.list().url);
    const promise = fetch('GET', url)
      .then((result) => {
        this.trigger({ contentPacks: result });

        return result;
      });

    ContentPacksActions.list.promise(promise);
  },

  create(request) {
    const promise = fetch('POST', URLUtils.qualifyUrl(ApiRoutes.ContentPacksController.create().url), request);

    ContentPacksActions.create.promise(promise);
  },
});

export default ContentPacksStores;
