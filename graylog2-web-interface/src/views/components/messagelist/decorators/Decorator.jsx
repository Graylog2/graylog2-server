import PropTypes from 'prop-types';
import React from 'react';
import createReactClass from 'create-react-class';

import { DropdownButton, MenuItem } from 'components/graylog';
import { ConfigurationForm, ConfigurationWell } from 'components/configurationforms';

import DecoratorStyles from '!style!css!./decoratorStyles.css';

const Decorator = createReactClass({
  displayName: 'Decorator',

  propTypes: {
    decorator: PropTypes.object.isRequired,
    typeDefinition: PropTypes.object.isRequired,
  },

  _handleDeleteClick() {
    const { onDelete, decorator } = this.props;
    if (window.confirm('Do you really want to delete this decorator?')) {
      onDelete(decorator.id);
    }
  },

  _handleEditClick() {
    this.editForm.open();
  },

  _handleSubmit(data) {
    const { stream, id, order } = this.props.decorator;
    const { onUpdate } = this.props;
    onUpdate(id, {
      type: data.type,
      config: data.configuration,
      order: order,
      stream: stream,
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
    const { decorator } = this.props;
    return (
      <DropdownButton id={`decorator-${decorator.id}-actions`} bsStyle="default" bsSize="xsmall" title="Actions" pullRight>
        <MenuItem onSelect={this._handleEditClick}>Edit</MenuItem>
        <MenuItem divider />
        <MenuItem onSelect={this._handleDeleteClick}>Delete</MenuItem>
      </DropdownButton>
    );
  },

  render() {
    const { decorator, decoratorTypes, typeDefinition } = this.props;
    const config = this._resolveConfigurationIds(decorator.config);
    const decoratorType = decoratorTypes[decorator.type] || this._decoratorTypeNotPresent();

    const decoratorActionsMenu = this._formatActionsMenu();
    const { name, requested_configuration: requestedConfiguration } = typeDefinition;
    return (
      <span className={DecoratorStyles.fixedWidth}>
        <div className={DecoratorStyles.decoratorBox}>
          <h6 className={DecoratorStyles.decoratorType}>{decoratorType.name}</h6>
          {decoratorActionsMenu}
        </div>
        <ConfigurationWell key={`configuration-well-decorator-${decorator.id}`}
                           id={decorator.id}
                           configuration={config}
                           typeDefinition={typeDefinition} />
        <ConfigurationForm ref={(editForm) => { this.editForm = editForm; }}
                           key="configuration-form-decorator"
                           configFields={requestedConfiguration}
                           title={`Edit ${name}`}
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
