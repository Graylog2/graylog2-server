import React, { PropTypes } from 'react';

import { Input } from 'components/bootstrap';
import ObjectUtils from 'util/ObjectUtils';
import FormsUtils from 'util/FormsUtils';

import { PluginStore } from 'graylog-web-plugin/plugin';

const DataAdapterForm = React.createClass({
  propTypes: {
    save: PropTypes.func.isRequired,
    type: PropTypes.string.isRequired,
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

  _saved() {
    if (this.props.create) {
      this.setState(this.getInitialState());
    }
  },
  _save(event) {
    if (event) {
      event.preventDefault();
    }

    this.props.save(this.state.dataAdapter, this._saved);
  },

  render() {
    const adapterPlugins = PluginStore.exports('lookupTableAdapters');

    const plugin = adapterPlugins.filter(p => p.type === this.props.type);
    let configFieldSet = null;
    if (plugin && plugin.length > 0) {
      configFieldSet = React.createElement(plugin[0].formComponent, {
        config: this.props.dataAdapter.config,
      });
    }

    const adapter = this.state.dataAdapter;
    return (<form className="form form-horizontal" onSubmit={this._save}>
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
               onChange={this._onChange}
               help="The name that is being used to refer to this data adapter. Must be unique."
               value={adapter.name}
               labelClassName="col-sm-3"
               wrapperClassName="col-sm-9" />
      </fieldset>
      {configFieldSet}
    </form>);
  },
});

export default DataAdapterForm;
