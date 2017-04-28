import React, { PropTypes } from 'react';

import { Button, Row, Col } from 'react-bootstrap';
import { Input } from 'components/bootstrap';
import ObjectUtils from 'util/ObjectUtils';
import FormsUtils from 'util/FormsUtils';

import { PluginStore } from 'graylog-web-plugin/plugin';

import CombinedProvider from 'injection/CombinedProvider';

const { LookupTableDataAdaptersActions } = CombinedProvider.get('LookupTableDataAdapters');

const DataAdapterForm = React.createClass({
  propTypes: {
    type: PropTypes.string.isRequired,
    saved: PropTypes.func.isRequired,
    create: PropTypes.bool,
    dataAdapter: PropTypes.object,
  },

  getDefaultProps() {
    return {
      create: true,
      dataAdapter: {
        id: undefined,
        title: '',
        description: '',
        name: '',
        config: {},
      },
    };
  },

  getInitialState() {
    const adapter = ObjectUtils.clone(this.props.dataAdapter);

    return {
      dataAdapter: {
        id: adapter.id,
        title: adapter.title,
        description: adapter.description,
        name: adapter.name,
        config: adapter.config,
      },
    };
  },

  _onChange(event) {
    const dataAdapter = ObjectUtils.clone(this.state.dataAdapter);
    dataAdapter[event.target.name] = FormsUtils.getValueFromInput(event.target);
    this.setState({ dataAdapter: dataAdapter });
  },

  _onConfigChange(event) {
    const dataAdapter = ObjectUtils.clone(this.state.dataAdapter);
    dataAdapter.config[event.target.name] = FormsUtils.getValueFromInput(event.target);
    this.setState({ dataAdapter: dataAdapter });
  },

  _updateConfig(newConfig) {
    const dataAdapter = ObjectUtils.clone(this.state.dataAdapter);
    dataAdapter.config = newConfig;
    this.setState({ dataAdapter: dataAdapter });
  },

  _save(event) {
    if (event) {
      event.preventDefault();
    }

    let promise;
    if (this.props.create) {
      promise = LookupTableDataAdaptersActions.create(this.state.dataAdapter);
    } else {
      promise = LookupTableDataAdaptersActions.update(this.state.dataAdapter);
    }

    promise.then(() => { this.props.saved(); });
  },

  render() {
    const adapter = this.state.dataAdapter;

    const adapterPlugins = PluginStore.exports('lookupTableAdapters');

    const plugin = adapterPlugins.filter(p => p.type === this.props.type);
    let configFieldSet = null;
    let documentationComponent = null;
    if (plugin && plugin.length > 0) {
      const p = plugin[0];
      configFieldSet = React.createElement(p.formComponent, {
        config: adapter.config,
        handleFormEvent: this._onConfigChange,
        updateConfig: this._updateConfig,
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
                     help="A short title for this data adapter."
                     value={adapter.title}
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9" />

              <Input type="text"
                     id="description"
                     name="description"
                     label="Description"
                     onChange={this._onChange}
                     help="Data adapter description."
                     value={adapter.description}
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9" />

              <Input type="text"
                     id="name"
                     name="name"
                     label="Name"
                     required
                     onChange={this._onChange}
                     help="The name that is being used to refer to this data adapter. Must be unique."
                     value={adapter.name}
                     labelClassName="col-sm-3"
                     wrapperClassName="col-sm-9" />
            </fieldset>
            {configFieldSet}
            <fieldset>
              <Input wrapperClassName="col-sm-offset-3 col-sm-9">
                <Button type="submit" bsStyle="success">{this.props.create ? 'Create Adapter' : 'Update Adapter'}</Button>
              </Input>
            </fieldset>
          </form>
        </Col>
        {documentationColumn}
      </Row>
    );
  },
});

export default DataAdapterForm;
