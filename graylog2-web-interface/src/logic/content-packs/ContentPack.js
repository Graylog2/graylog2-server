import { Map } from 'immutable';

export default class ContentPack {
  constructor(version, id, revision, name, summary, description, vendor, url, requires,
    parameters, entities) {
    this._value = {
      version,
      id,
      revision,
      name,
      summary,
      description,
      vendor,
      url,
      requires,
      parameters,
      entities,
    };
  }

  get version() {
    return this._value.version;
  }

  get id() {
    return this._value.id;
  }

  get revision() {
    return this._value.revision;
  }

  get name() {
    return this._value.name;
  }

  get summary() {
    return this._value.summary;
  }

  get description() {
    return this._value.description;
  }

  get vendor() {
    return this._value.vendor;
  }

  get url() {
    return this._value.url;
  }

  get requires() {
    return this._value.requires;
  }

  get parameters() {
    return this._value.parameters;
  }

  get entities() {
    return this._value.entities;
  }

  toBuilder() {
    const {
      version,
      id,
      revision,
      name,
      summary,
      description,
      vendor,
      url,
      requires,
      parameters,
      entities,
    } = this._value;
    // eslint-disable-next-line no-use-before-define
    return new Builder(Map({
      version,
      id,
      revision,
      name,
      summary,
      description,
      vendor,
      url,
      requires,
      parameters,
      entities,
    }));
  }

  static fromJSON(value) {
    const {
      version,
      id,
      revision,
      name,
      summary,
      description,
      vendor,
      url,
      requires,
      parameters,
      entities,
    } = value;
    return new ContentPack(
      version,
      id,
      revision,
      name,
      summary,
      description,
      vendor,
      url,
      requires,
      parameters,
      entities,
    )
  }

  static builder() {
    // eslint-disable-next-line no-use-before-define
    return new Builder();
  }
}

class Builder {
  constructor(value = Map()) {
    this.value = value;
  }

  version(value) {
    this.value = this.value.set('version', value);
    return this;
  }

  id(value) {
    this.value = this.value.set('id', value);
    return this;
  }

  revision(value) {
    this.value = this.value.set('revision', value);
    return this;
  }

  name(value) {
    this.value = this.value.set('name', value);
    return this;
  }

  summary(value) {
    this.value = this.value.set('summary', value);
    return this;
  }

  description(value) {
    this.value = this.value.set('description', value);
    return this;
  }

  vendor(value) {
    this.value = this.value.set('vendor', value);
    return this;
  }

  url(value) {
    this.value = this.value.set('url', value);
    return this;
  }

  requires(value) {
    this.value = this.value.set('requires', value);
    return this;
  }

  parameters(value) {
    this.value = this.value.set('parameters', value);
    return this;
  }

  entities(value) {
    this.value = this.value.set('entities', value);
    return this;
  }

  build() {
    const {
      version,
      id,
      revision,
      name,
      summary,
      description,
      vendor,
      url,
      requires,
      parameters,
      entities,
    } = this.value.toObject();
    return new ContentPack(
      version,
      id,
      revision,
      name,
      summary,
      description,
      vendor,
      url,
      requires,
      parameters,
      entities,
    );
  }
}
