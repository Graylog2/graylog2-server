import React from 'react';
import Reflux from 'reflux';

import { DropdownButton, MenuItem } from 'react-bootstrap';

import { Spinner } from 'components/common';
import { ConfigurationForm, ConfigurationWell } from 'components/configurationforms';

import StoreProvider from 'injection/StoreProvider';
const DecoratorsStore = StoreProvider.getStore('Decorators');
const CurrentUserStore = StoreProvider.getStore('CurrentUser');

import ActionsProvider from 'injection/ActionsProvider';
const DecoratorsActions = ActionsProvider.getActions('Decorators');

import PermissionsMixin from 'util/PermissionsMixin';

import DecoratorStyles from '!style!css!components/search/decoratorStyles.css';

const Decorator = React.createClass({
  propTypes: {
    decorator: React.PropTypes.object.isRequired,
    typeDefinition: React.PropTypes.object.isRequired,
  },
  mixins: [Reflux.connect(DecoratorsStore), Reflux.connect(CurrentUserStore), PermissionsMixin],
  componentDidMount() {
    DecoratorsActions.available();
  },

  _handleDeleteClick() {
    if (window.confirm('Do you really want to delete this decorator?')) {
      DecoratorsActions.remove(this.props.decorator.id);
    }
  },
  _handleEditClick() {
    this.refs.editForm.open();
  },
  _handleSubmit(data) {
    DecoratorsActions.update(this.props.decorator.id, {
      type: data.type,
      config: data.configuration,
      order: this.props.decorator.order,
      stream: this.props.decorator.stream,
    });
  },
  _decoratorTypeNotPresent() {
    return {
      name: 'Unknown decorator type',
    };
  },
  // Attempts to resolve ID values in the decorator configuration against the type definition.
  // This allows users to see actual names for entities in drop-downs, instead of their IDs.
  _resolveConfigurationIds(config) {
    const typeConfig = this.props.typeDefinition.requested_configuration;
    const resolvedConfig = {};
    const configKeys = Object.keys(config);

    configKeys.forEach((key) => {
      const configValues = (typeConfig[key] ? typeConfig[key].additional_info.values : undefined);
      const originalValue = config[key];
      if (configValues) {
        if (configValues[originalValue]) {
          resolvedConfig[key] = configValues[originalValue];
        }
      }
    });

    return Object.assign({}, config, resolvedConfig);
  },
  _formatActionsMenu() {
    const permissions = this.state.currentUser.permissions;
    const decorator = this.props.decorator;
    const editPermission = this.isPermitted(permissions, `decorators:edit:${decorator.stream}`);
    return (
      <DropdownButton id={`decorator-${decorator.id}-actions`} bsStyle="default" bsSize="xsmall" title="Actions" pullRight>
        <MenuItem onSelect={this._handleEditClick} disabled={!editPermission}>Edit</MenuItem>
        <MenuItem divider />
        <MenuItem onSelect={this._handleDeleteClick} disabled={!editPermission}>Delete</MenuItem>
      </DropdownButton>
    );
  },
  render() {
    if (!this.state.types || !this.state.currentUser) {
      return <Spinner />;
    }
    const decorator = this.props.decorator;
    const config = this._resolveConfigurationIds(decorator.config);
    const decoratorType = this.state.types[decorator.type] || this._decoratorTypeNotPresent();

    const decoratorActionsMenu = this._formatActionsMenu();
    return (
      <span className={DecoratorStyles.fullWidth}>
        <div className={DecoratorStyles.decoratorBox}>
          <h6 className={DecoratorStyles.decoratorType}>{decoratorType.name}</h6>
          {decoratorActionsMenu}
        </div>
        <ConfigurationWell key={`configuration-well-decorator-${decorator.id}`}
                           id={decorator.id}
                           configuration={config}
                           typeDefinition={this.props.typeDefinition} />
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
