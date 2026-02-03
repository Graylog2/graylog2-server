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
import * as React from 'react';
import { useCallback, useMemo, useState } from 'react';
import styled, { css } from 'styled-components';

import { ConfigurationForm } from 'components/configurationforms';
import type { ConfigurationFormData } from 'components/configurationforms';
import { Select } from 'components/common';
import type { DecoratorType, Decorator } from 'views/components/messagelist/decorators/Types';
import generateObjectId from 'logic/generateObjectId';

import InlineForm from './InlineForm';
import PopoverHelp from './PopoverHelp';
import DecoratorStyles from './decoratorStyles.css';

const ConfigurationFormContainer = styled.div(
  ({ theme }) => css`
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
  `,
);

type Props = {
  disabled?: boolean;
  decoratorTypes: { [key: string]: DecoratorType };
  nextOrder: number;
  stream?: string;
  onCreate: (newDecorator: Decorator) => void;
  showHelp?: boolean;
};

const _formatDecoratorType = (typeDefinition: DecoratorType, typeName: string) => ({
  value: typeName,
  label: typeDefinition.name,
});

const AddDecoratorButton = ({
  decoratorTypes,
  disabled = false,
  showHelp = true,
  stream = null,
  nextOrder,
  onCreate,
}: Props) => {
  const [typeName, setTypeName] = useState<string | undefined>(undefined);
  const [typeDefinition, setTypeDefinition] = useState<DecoratorType | undefined>(undefined);
  const _handleCancel = () => {
    setTypeDefinition(undefined);
    setTypeName(undefined);
  };

  const _handleSubmit = useCallback(
    (data: ConfigurationFormData<Decorator['config']>) => {
      const request = {
        id: generateObjectId(),
        stream,
        type: data.type,
        config: data.configuration,
        order: nextOrder,
      };

      onCreate(request);
      setTypeName(undefined);
    },
    [nextOrder, onCreate, stream],
  );

  const _onTypeChange = useCallback(
    (decoratorType: string) => {
      setTypeName(decoratorType);
      setTypeDefinition(decoratorTypes[decoratorType] ?? ({} as DecoratorType));
    },
    [decoratorTypes],
  );

  const decoratorTypeOptions = useMemo(
    () => Object.entries(decoratorTypes).map(([name, type]) => _formatDecoratorType(type, name)),
    [decoratorTypes],
  );
  const wrapperComponent = InlineForm();
  const configurationForm =
    typeName !== undefined ? (
      <ConfigurationForm<Decorator['config']>
        key="configuration-form-output"
        configFields={typeDefinition.requested_configuration}
        title={`Create new ${typeDefinition.name}`}
        typeName={typeName}
        includeTitleField={false}
        wrapperComponent={wrapperComponent as React.ComponentProps<typeof ConfigurationForm>['wrapperComponent']}
        submitAction={_handleSubmit}
        cancelAction={_handleCancel}
      />
    ) : null;

  return (
    <>
      <div className={`${DecoratorStyles.decoratorBox} ${DecoratorStyles.addDecoratorButtonContainer}`}>
        <div className={DecoratorStyles.addDecoratorSelect}>
          <Select
            placeholder="Select decorator"
            onChange={_onTypeChange}
            options={decoratorTypeOptions}
            disabled={disabled}
            value={typeName}
          />
        </div>
      </div>
      {showHelp && <PopoverHelp />}

      {typeName && <ConfigurationFormContainer>{configurationForm}</ConfigurationFormContainer>}
    </>
  );
};

export default AddDecoratorButton;
