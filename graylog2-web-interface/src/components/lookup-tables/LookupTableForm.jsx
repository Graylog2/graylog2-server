import React, { PropTypes } from 'react';

import { Button } from 'react-bootstrap';
// import { LinkContainer } from 'react-router';
import { Input } from 'components/bootstrap';
import ObjectUtils from 'util/ObjectUtils';
import FormsUtils from 'util/FormsUtils';

import { PluginStore } from 'graylog-web-plugin/plugin';

import { CachesContainer, CachePicker, DataAdaptersContainer, DataAdapterPicker } from 'components/lookup-tables';

import CombinedProvider from 'injection/CombinedProvider';

const { LookupTablesActions } = CombinedProvider.get('LookupTables');

const LookupTableForm = React.createClass({
  propTypes: {
    saved: PropTypes.func.isRequired,
    create: PropTypes.bool,
    table: PropTypes.object,
  },

  getDefaultProps() {
    return {
      create: true,
      table: {
        id: undefined,
        title: '',
        description: '',
        name: '',
        cache_id: undefined,
        data_adapter_id: undefined,
      },
    };
  },

  getInitialState() {
    const table = ObjectUtils.clone(this.props.table);

    return {
      table: {
        id: table.id,
        title: table.title,
        description: table.description,
        name: table.name,
        cache_id: table.cache_id,
        data_adapter_id: table.data_adapter_id,
      },
    };
  },

  _onChange(event) {
    const table = ObjectUtils.clone(this.state.table);
    table[event.target.name] = FormsUtils.getValueFromInput(event.target);
    this.setState({ table: table });
  },

  _onConfigChange(event) {
    const table = ObjectUtils.clone(this.state.table);
    table.config[event.target.name] = FormsUtils.getValueFromInput(event.target);
    this.setState({ table: table });
  },

  _save(event) {
    if (event) {
      event.preventDefault();
    }

    let promise;
    if (this.props.create) {
      promise = LookupTablesActions.create(this.state.table);
    } else {
      promise = LookupTablesActions.update(this.state.table);
    }

    promise.then(() => {
      this.props.saved();
    });
  },

  _onAdapterSelect(id) {
    const table = ObjectUtils.clone(this.state.table);
    table.data_adapter_id = id;
    this.setState({ table: table });
  },

  _onCacheSelect(id) {
    const table = ObjectUtils.clone(this.state.table);
    table.cache_id = id;
    this.setState({ table: table });
  },

  render() {
    const table = this.state.table;

    return (
      <form className="form form-horizontal" onSubmit={this._save}>
        <fieldset>
          <Input type="text"
                 id="title"
                 name="title"
                 label="Title"
                 autoFocus
                 required
                 onChange={this._onChange}
                 help="A short title for this lookup table."
                 value={table.title}
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-9" />

          <Input type="text"
                 id="description"
                 name="description"
                 label="Description"
                 onChange={this._onChange}
                 help="Description of the lookup table."
                 value={table.description}
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-9" />

          <Input type="text"
                 id="name"
                 name="name"
                 label="Name"
                 required
                 onChange={this._onChange}
                 help="The name that is being used to refer to this lookup table. Must be unique."
                 value={table.name}
                 labelClassName="col-sm-3"
                 wrapperClassName="col-sm-9" />
        </fieldset>

        <DataAdaptersContainer>
          <DataAdapterPicker onSelect={this._onAdapterSelect} selectedId={this.state.table.data_adapter_id} />
        </DataAdaptersContainer>

        <CachesContainer>
          <CachePicker onSelect={this._onCacheSelect} selectedId={this.state.table.cache_id} />
        </CachesContainer>

        <fieldset>
          <Input wrapperClassName="col-sm-offset-3 col-sm-9">
            <Button type="submit" bsStyle="success">{this.props.create ? 'Create Lookup Table' : 'Update Lookup Table'}</Button>
          </Input>
        </fieldset>
      </form>
    );
  },
});

export default LookupTableForm;
