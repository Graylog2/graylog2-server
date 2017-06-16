import React, { PropTypes } from 'react';
import _ from 'lodash';
import { Button } from 'react-bootstrap';
// import { LinkContainer } from 'react-router';
import { Input } from 'components/bootstrap';
import ObjectUtils from 'util/ObjectUtils';
import FormsUtils from 'util/FormsUtils';
import { JSONValueInput } from 'components/common';

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
        default_single_value: '',
        default_single_value_type: '',
        default_multi_value: '',
        default_multi_value_type: '',
      },
    };
  },

  getInitialState() {
    return this._initialState(this.props.table);
  },

  componentWillReceiveProps(nextProps) {
    if (_.isEqual(this.props, nextProps)) {
      // props haven't change, don't update our state from them
      return;
    }
    this.setState(this._initialState(nextProps.table));
  },

  _initialState(t) {
    const table = ObjectUtils.clone(t);

    return {
      table: {
        id: table.id,
        title: table.title,
        description: table.description,
        name: table.name,
        cache_id: table.cache_id,
        data_adapter_id: table.data_adapter_id,
        default_single_value: table.default_single_value,
        default_single_value_type: table.default_single_value_type,
        default_multi_value: table.default_multi_value,
        default_multi_value_type: table.default_multi_value_type,
      },
      enable_default_single: table.default_single_value_type && table.default_single_value_type !== 'NULL',
      enable_default_multi: table.default_multi_value_type && table.default_multi_value_type !== 'NULL',
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

  _onDefaultValueUpdate(name, value, valueType) {
    const table = ObjectUtils.clone(this.state.table);

    table[`default_${name}_value`] = value;
    table[`default_${name}_value_type`] = valueType;

    this.setState({ table: table });
  },

  _onCheckEnableSingleDefault(e) {
    const value = FormsUtils.getValueFromInput(e.target);
    this.setState({ enable_default_single: value });

    if (value === false) {
      this._onDefaultValueUpdate('single', '', 'NULL');
    }
  },

  _onCheckEnableMultiDefault(e) {
    const value = FormsUtils.getValueFromInput(e.target);
    this.setState({ enable_default_multi: value });

    if (value === false) {
      this._onDefaultValueUpdate('multi', '', 'NULL');
    }
  },

  _onDefaultSingleValueUpdate(value, valueType) {
    this._onDefaultValueUpdate('single', value, valueType);
  },

  _onDefaultMultiValueUpdate(value, valueType) {
    this._onDefaultValueUpdate('multi', value, valueType);
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

          <Input type="checkbox"
                 label="Enable single default value"
                 checked={this.state.enable_default_single}
                 onChange={this._onCheckEnableSingleDefault}
                 help="Enable if the lookup table should provide a default for the single value."
                 wrapperClassName="col-md-offset-3 col-md-9"
                 />

          {this.state.enable_default_single &&
          <JSONValueInput label="Default single value"
                          help="The single value that is being used as lookup result if the data adapter or cache does not find a value."
                          update={this._onDefaultSingleValueUpdate}
                          required
                          value={table.default_single_value}
                          valueType={table.default_single_value_type || 'NULL'}
                          allowedTypes={['STRING', 'NUMBER', 'BOOLEAN', 'NULL']}
                          labelClassName="col-sm-3"
                          wrapperClassName="col-sm-9" />
          }

          <Input type="checkbox"
                 label="Enable multi default value"
                 checked={this.state.enable_default_multi}
                 onChange={this._onCheckEnableMultiDefault}
                 help="Enable if the lookup table should provide a default for the multi value."
                 wrapperClassName="col-md-offset-3 col-md-9" />

          {this.state.enable_default_multi &&
          <JSONValueInput label="Default multi value"
                          help="The multi value that is being used as lookup result if the data adapter or cache does not find a value."
                          update={this._onDefaultMultiValueUpdate}
                          value={table.default_multi_value}
                          valueType={table.default_multi_value_type || 'NULL'}
                          allowedTypes={['OBJECT', 'NULL']}
                          labelClassName="col-sm-3"
                          wrapperClassName="col-sm-9" />
          }
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
