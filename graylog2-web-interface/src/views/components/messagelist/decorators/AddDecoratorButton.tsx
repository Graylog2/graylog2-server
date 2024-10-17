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
import React from 'react';
import jQuery from 'jquery';
import styled, { css } from 'styled-components';
import ObjectID from 'bson-objectid';

import { ConfigurationForm } from 'components/configurationforms';
import type { ConfigurationFormData } from 'components/configurationforms';
import { Select } from 'components/common';
import type { DecoratorType, Decorator } from 'views/components/messagelist/decorators/Types';

import InlineForm from './InlineForm';
import PopoverHelp from './PopoverHelp';
import DecoratorStyles from './decoratorStyles.css';

const ConfigurationFormContainer = styled.div(({ theme }) => css`
  margin-bottom: 10px;
  margin-top: 10px;
  margin-left: 5px;
  display: inline-block;
  border-style: solid;
  border-color: ${theme.colors.gray[80]};
  border-radius: 5px;
  border-width: 1px;
  padding: 10px;
  background: ${theme.colors.global.background};
`);

type Props = {
  disabled?: boolean,
  decoratorTypes: { [key: string]: DecoratorType },
  nextOrder: number,
  stream?: string,
  onCreate: (newDecorator: Decorator) => void,
  showHelp?: boolean,
};

type State = {
  typeDefinition?: DecoratorType,
  typeName?: string,
};

class AddDecoratorButton extends React.Component<Props, State> {
  static defaultProps = {
    disabled: false,
    showHelp: true,
    stream: null,
  };

  private configurationForm: any;

  constructor(props: Props) {
    super(props);

    this.configurationForm = React.createRef();

    this.state = {};
  }

  // eslint-disable-next-line class-methods-use-this
  _formatDecoratorType = (typeDefinition: DecoratorType, typeName: string) => ({ value: typeName, label: typeDefinition.name });

  _handleCancel = () => this.setState({ typeName: undefined, typeDefinition: undefined });

  _handleSubmit = (data: ConfigurationFormData<Decorator['config']>) => {
    const { stream, nextOrder, onCreate } = this.props;

    const request = {
      id: new ObjectID().toString(),
      stream,
      type: data.type,
      config: data.configuration,
      order: nextOrder,
    };

    onCreate(request);
    this.setState({ typeName: undefined });
  };

  // eslint-disable-next-line react/no-unused-class-component-methods
  _openModal = () => this.configurationForm?.current?.open();

  _onTypeChange = (decoratorType) => {
    const { decoratorTypes } = this.props;

    this.setState({ typeName: decoratorType });

    if (decoratorTypes[decoratorType]) {
      this.setState({ typeDefinition: decoratorTypes[decoratorType] });
    } else {
      this.setState({ typeDefinition: {} as DecoratorType });
    }
  };

  render() {
    const { typeDefinition, typeName } = this.state;
    const { decoratorTypes, disabled, showHelp = true } = this.props;

    const decoratorTypeOptions = jQuery.map(decoratorTypes, this._formatDecoratorType);
    const wrapperComponent = InlineForm();
    const configurationForm = (typeName !== undefined
      ? (
        <ConfigurationForm<Decorator['config']> ref={this.configurationForm}
                                                key="configuration-form-output"
                                                configFields={typeDefinition.requested_configuration}
                                                title={`Create new ${typeDefinition.name}`}
                                                typeName={typeName}
                                                includeTitleField={false}
                                                wrapperComponent={wrapperComponent as React.ComponentProps<typeof ConfigurationForm>['wrapperComponent']}
                                                submitAction={this._handleSubmit}
                                                cancelAction={this._handleCancel} />
      ) : null);

    return (
      <>
        <div className={`${DecoratorStyles.decoratorBox} ${DecoratorStyles.addDecoratorButtonContainer}`}>
          <div className={DecoratorStyles.addDecoratorSelect}>
            <Select placeholder="Select decorator"
                    onChange={this._onTypeChange}
                    options={decoratorTypeOptions}
                    matchProp="label"
                    disabled={disabled}
                    value={typeName} />
          </div>
        </div>
        {showHelp && <PopoverHelp />}

        {typeName && <ConfigurationFormContainer>{configurationForm}</ConfigurationFormContainer>}
      </>
    );
  }
}

export default AddDecoratorButton;
