/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import PropTypes from 'prop-types';
import React from 'react';

import { DropdownButton, MenuItem } from 'components/graylog';
import { ConfigurationForm, ConfigurationWell } from 'components/configurationforms';

// eslint-disable-next-line import/no-webpack-loader-syntax
import InlineForm from './InlineForm';

import DecoratorStyles from '!style-loader!css-loader!./decoratorStyles.css';

class DecoratorSummary extends React.Component {
  static propTypes = {
    decorator: PropTypes.object.isRequired,
    decoratorTypes: PropTypes.object.isRequired,
    disableMenu: PropTypes.bool,
    typeDefinition: PropTypes.object.isRequired,
    onDelete: PropTypes.func.isRequired,
    onUpdate: PropTypes.func.isRequired,
  };

  static defaultProps = {
    disableMenu: false,
  };

  constructor(props) {
    super(props);

    this.state = {
      editing: false,
    };
  }

  _handleDeleteClick = () => {
    const { onDelete, decorator } = this.props;

    // eslint-disable-next-line no-alert
    if (window.confirm('Do you really want to delete this decorator?')) {
      onDelete(decorator.id);
    }
  };

  _handleEditClick = () => {
    this.setState({ editing: true });
  };

  _closeEditForm = () => {
    this.setState({ editing: false });
  };

  _handleSubmit = (data) => {
    const { decorator } = this.props;
    const { stream, id, order } = decorator;
    const { onUpdate } = this.props;

    onUpdate(id, {
      id,
      type: data.type,
      config: data.configuration,
      order: order,
      stream: stream,
    });

    this._closeEditForm();
  };

  _decoratorTypeNotPresent = () => {
    return {
      name: 'Unknown decorator type',
    };
  };

  // Attempts to resolve ID values in the decorator configuration against the type definition.
  // This allows users to see actual names for entities in drop-downs, instead of their IDs.
  _resolveConfigurationIds = (config) => {
    const { typeDefinition } = this.props;
    const typeConfig = typeDefinition.requested_configuration;
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

    return { ...config, ...resolvedConfig };
  };

  _formatActionsMenu = () => {
    const { decorator } = this.props;

    return (
      <DropdownButton id={`decorator-${decorator.id}-actions`} bsStyle="default" bsSize="xsmall" title="Actions" pullRight>
        <MenuItem onSelect={this._handleEditClick}>Edit</MenuItem>
        <MenuItem divider />
        <MenuItem onSelect={this._handleDeleteClick}>Delete</MenuItem>
      </DropdownButton>
    );
  };

  render() {
    const { disableMenu = false, decorator, decoratorTypes, typeDefinition } = this.props;
    const { editing } = this.state;
    const config = this._resolveConfigurationIds(decorator.config);
    const decoratorType = decoratorTypes[decorator.type] || this._decoratorTypeNotPresent();

    const decoratorActionsMenu = disableMenu || this._formatActionsMenu();
    const { name, requested_configuration: requestedConfiguration } = typeDefinition;
    const wrapperComponent = InlineForm('Update');
    const decoratorId = decorator.id || 'new';

    const content = editing
      ? (
        <ConfigurationForm key="configuration-form-decorator"
                           configFields={requestedConfiguration}
                           title={`Edit ${name}`}
                           typeName={decorator.type}
                           includeTitleField={false}
                           submitAction={this._handleSubmit}
                           cancelAction={this._closeEditForm}
                           wrapperComponent={wrapperComponent}
                           values={decorator.config} />
      )
      : (
        <ConfigurationWell key={`configuration-well-decorator-${decoratorId}`}
                           id={decoratorId}
                           configuration={config}
                           typeDefinition={typeDefinition} />
      );

    return (
      <span className={DecoratorStyles.fixedWidth}>
        <div className={DecoratorStyles.decoratorBox}>
          <h6 className={DecoratorStyles.decoratorType}>{decoratorType.name}</h6>
          {decoratorActionsMenu}
        </div>
        {content}
      </span>
    );
  }
}

export default DecoratorSummary;
