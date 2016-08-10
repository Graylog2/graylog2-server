import React from 'react';
import Reflux from 'reflux';

import { DropdownButton, MenuItem } from 'react-bootstrap';

import { Spinner } from 'components/common';
import { ConfigurationForm, ConfigurationWell } from 'components/configurationforms';

import StoreProvider from 'injection/StoreProvider';
const DecoratorsStore = StoreProvider.getStore('Decorators');

import ActionsProvider from 'injection/ActionsProvider';
const DecoratorsActions = ActionsProvider.getActions('Decorators');

import DecoratorStyles from '!style!css!components/search/decoratorStyles.css';

const Decorator = React.createClass({
  propTypes: {
    decorator: React.PropTypes.object.isRequired,
    typeDefinition: React.PropTypes.object.isRequired,
  },
  mixins: [Reflux.connect(DecoratorsStore)],
  componentDidMount() {
    DecoratorsActions.available();
  },

  _handleDeleteClick() {
    if (window.confirm('Do you really want to delete this decorator?')) {
      DecoratorsActions.remove(this.props.decorator._id);
    }
  },
  _handleEditClick() {
    this.refs.editForm.open();
  },
  _handleSubmit(data) {
    DecoratorsActions.update(this.props.decorator._id, { type: data.type, config: data.configuration });
  },
  _decoratorTypeNotPresent() {
    return {
      name: <strong>Unknown decorator type</strong>,
    };
  },
  render() {
    if (!this.state.types) {
      return <Spinner />;
    }
    const decorator = this.props.decorator;
    const decoratorType = this.state.types[decorator.type] || this._decoratorTypeNotPresent();
    return (
      <span className={DecoratorStyles.fullWidth}>
        <div className={DecoratorStyles.decoratorBox}>
          <h6 className={DecoratorStyles.decoratorType}>{decoratorType.name}</h6>
          <DropdownButton id={`decorator-${decorator._id}-actions`} bsStyle="default" bsSize="xsmall" title="Actions" pullRight>
            <MenuItem onSelect={this._handleEditClick}>Edit</MenuItem>
            <MenuItem divider/>
            <MenuItem onSelect={this._handleDeleteClick}>Delete</MenuItem>
          </DropdownButton>
        </div>
        <ConfigurationWell key={`configuration-well-decorator-${decorator._id}`}
                           id={decorator._id}
                           configuration={decorator.config}
                           typeDefinition={this.props.typeDefinition}/>
        <ConfigurationForm ref="editForm"
                           key="configuration-form-decorator"
                           configFields={this.props.typeDefinition.requested_configuration}
                           title={`Edit ${this.props.typeDefinition.name}`}
                           typeName={decorator.type}
                           includeTitleField={false}
                           submitAction={this._handleSubmit}
                           cancelAction={this._handleCancel}
                           values={decorator.config} />
      </span>
    );
  },
});

export default Decorator;
