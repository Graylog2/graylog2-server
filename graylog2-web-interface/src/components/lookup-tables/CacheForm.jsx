import React, { PropTypes } from 'react';
import _ from 'lodash';
import { Button, Col, Row } from 'react-bootstrap';
import { Input } from 'components/bootstrap';
import ObjectUtils from 'util/ObjectUtils';
import FormsUtils from 'util/FormsUtils';

import { PluginStore } from 'graylog-web-plugin/plugin';

import CombinedProvider from 'injection/CombinedProvider';

const { LookupTableCachesActions } = CombinedProvider.get('LookupTableCaches');

const CacheForm = React.createClass({
  propTypes: {
    type: PropTypes.string.isRequired,
    saved: PropTypes.func.isRequired,
    create: PropTypes.bool,
    cache: PropTypes.object,
    validate: PropTypes.func,
    validationErrors: PropTypes.object,
  },

  getDefaultProps() {
    return {
      create: true,
      cache: {
        id: undefined,
        title: '',
        description: '',
        name: '',
        config: {},
      },
      validate: null,
      validationErrors: {},
    };
  },

  getInitialState() {
    return this._initialState(this.props.cache);
  },

  componentWillReceiveProps(nextProps) {
    if (_.isEqual(this.props.cache, nextProps.cache)) {
      // props haven't change, don't update our state from them
      return;
    }
    this.setState(this._initialState(nextProps.cache));
  },

  componentDidMount() {
    if (!this.props.create) {
      // Validate when mounted to immediately show errors for invalid objects
      this._validate(this.props.cache);
    }
  },

  _initialState(c) {
    const cache = ObjectUtils.clone(c);

    return {
      // when creating always initially auto-generate the adapter name,
      // this will be false if the user changed the adapter name manually
      generateName: this.props.create,
      cache: {
        id: cache.id,
        title: cache.title,
        description: cache.description,
        name: cache.name,
        config: cache.config,
      },
    };
  },

  componentWillUnmount() {
    this._clearTimer();
  },

  validationCheckTimer: undefined,

  _clearTimer() {
    if (this.validationCheckTimer !== undefined) {
      clearTimeout(this.validationCheckTimer);
      this.validationCheckTimer = undefined;
    }
  },

  _validate(cache) {
    // first cancel outstanding validation timer, we have new data
    this._clearTimer();
    if (this.props.validate) {
      this.validationCheckTimer = setTimeout(() => this.props.validate(cache), 500);
    }
  },

  _onChange(event) {
    const cache = ObjectUtils.clone(this.state.cache);
    cache[event.target.name] = FormsUtils.getValueFromInput(event.target);
    let generateName = this.state.generateName;
    if (generateName && event.target.name === 'title') {
      // generate the name
      cache.name = this._sanitizeTitle(cache.title);
    }
    if (event.target.name === 'name') {
      // the cache name has been changed manually, no longer automatically change it
      generateName = false;
    }
    this._validate(cache);
    this.setState({ cache: cache });
  },

  _onConfigChange(event) {
    const cache = ObjectUtils.clone(this.state.cache);
    cache.config[event.target.name] = FormsUtils.getValueFromInput(event.target);
    this._validate(cache);
    this.setState({ cache: cache });
  },

  _updateConfig(newConfig) {
    const cache = ObjectUtils.clone(this.state.cache);
    cache.config = newConfig;
    this._validate(cache);
    this.setState({ cache: cache });
  },

  _save(event) {
    if (event) {
      event.preventDefault();
    }

    let promise;
    if (this.props.create) {
      promise = LookupTableCachesActions.create(this.state.cache);
    } else {
      promise = LookupTableCachesActions.update(this.state.cache);
    }

    promise.then(() => { this.props.saved(); });
  },

  _sanitizeTitle(title) {
    return title.trim().replace(/\W+/g, '-').toLowerCase();
  },

  _validationState(fieldName) {
    if (this.props.validationErrors[fieldName]) {
      return 'error';
    }
    return null;
  },

  _validationMessage(fieldName, defaultText) {
    if (this.props.validationErrors[fieldName]) {
      return (<div>
        <span>{defaultText}</span>
        &nbsp;
        <span><b>{this.props.validationErrors[fieldName][0]}</b></span>
      </div>);
    }
    return <span>{defaultText}</span>;
  },

  render() {
    const cache = this.state.cache;

    const cachePlugins = PluginStore.exports('lookupTableCaches');

    const plugin = cachePlugins.filter(p => p.type === this.props.type);
    let configFieldSet = null;
    let documentationComponent = null;
    if (plugin && plugin.length > 0) {
      const p = plugin[0];
      configFieldSet = React.createElement(p.formComponent, {
        config: cache.config,
        handleFormEvent: this._onConfigChange,
        updateConfig: this._updateConfig,
        validationMessage: this._validationMessage,
        validationState: this._validationState,
      });
      if (p.documentationComponent) {
        documentationComponent = React.createElement(p.documentationComponent);
      }
    }

    let documentationColumn = null;
    let formRowWidth = 8; // If there is no documentation component, we don't use the complete page width
    if (documentationComponent) {
      formRowWidth = 6;
      documentationColumn = (
        <Col lg={formRowWidth}>
          {documentationComponent}
        </Col>
      );
    }

    return (
      <Row>
        <Col lg={formRowWidth}>
          <form className="form form-horizontal" onSubmit={this._save}>
            <fieldset>
              <Input type="text"
                     id="title"
                     name="title"
                     label="Title"
                     autoFocus
                     required
                     onChange={this._onChange}
                     help="A short title for this cache."
                     value={cache.title}
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9" />

              <Input type="text"
                     id="description"
                     name="description"
                     label="Description"
                     onChange={this._onChange}
                     help="Cache description."
                     value={cache.description}
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9" />

              <Input type="text"
                     id="name"
                     name="name"
                     label="Name"
                     required
                     onChange={this._onChange}
                     help={this._validationMessage('name', 'The name that is being used to refer to this cache. Must be unique.')}
                     bsStyle={this._validationState('name')}
                     value={cache.name}
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9" />
            </fieldset>
            {configFieldSet}
            <fieldset>
              <Input wrapperClassName="col-sm-offset-3 col-sm-9">
                <Button type="submit" bsStyle="success">{this.props.create ? 'Create Cache' : 'Update Cache'}</Button>
              </Input>
            </fieldset>
          </form>
        </Col>
        {documentationColumn}
      </Row>
    );
  },
});

export default CacheForm;
