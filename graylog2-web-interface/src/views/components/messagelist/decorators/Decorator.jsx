import PropTypes from 'prop-types';
import React from 'react';

import { DropdownButton, MenuItem } from 'components/graylog';
import { ConfigurationForm, ConfigurationWell } from 'components/configurationforms';

import DecoratorStyles from '!style!css!./decoratorStyles.css';
import InlineForm from './InlineForm';

class Decorator extends React.Component {
  static propTypes = {
    decorator: PropTypes.object.isRequired,
    typeDefinition: PropTypes.object.isRequired,
  };

  constructor(props) {
    super(props);
    this.state = {
      editing: false,
    };
  }

  _handleDeleteClick = () => {
    const { onDelete, decorator } = this.props;
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
    const { stream, id, order } = this.props.decorator;
    const { onUpdate } = this.props;
    onUpdate(id, {
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
        <ConfigurationWell key={`configuration-well-decorator-${decorator.id}`}
                           id={decorator.id}
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

export default Decorator;
