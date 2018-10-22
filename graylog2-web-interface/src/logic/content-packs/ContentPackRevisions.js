import { max } from 'lodash';
import ContentPack from 'logic/content-packs/ContentPack';

export default class ContentPackRevisions {
  constructor(contentPackRevision) {
    this._value = Object.keys(contentPackRevision).reduce((acc, rev) => {
      const contentPack = contentPackRevision[rev];
      /* eslint-disable-next-line no-return-assign */
      return acc[parseInt(rev, 10)] = new ContentPack.fromJSON(contentPack);
    }, {});
  }

  get latestRevision() {
    return max(this.revisions);
  }

  get revisions() {
    return Object.keys(this._value);
  }

  get latest() {
    return this._value[this.latestRevision];
  }

  get contentPacks() {
    return Object.values(this._value);
  }

  contentPack(revision) {
    return this._value[revision];
  }
}
