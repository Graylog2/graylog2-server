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
import styled, { css } from 'styled-components';

import { Well } from 'components/bootstrap';
import type {
  ConfigurationField,
  ConfigurationFieldValue,
  EncryptedFieldValue,
} from 'components/configurationforms/types';

const PASSWORD_PLACEHOLDER = '********';

type Props = {
  id: string;
  configuration: {
    [key: string]: ConfigurationFieldValue;
  };
  typeDefinition?: {
    requested_configuration: {
      [key: string]: ConfigurationField;
    };
  };
};

const RegularField = ({ id, value, name }: { id: string; value: ConfigurationFieldValue; name: string }) => {
  let finalValue;

  if (value === null || value === undefined || value === '' || (Array.isArray(value) && value.length === 0)) {
    finalValue = <i>{'<empty>'}</i>;
  } else {
    finalValue = Array.isArray(value) ? value.join(', ') : String(value);
  }

  return (
    <li key={`${id}-${name}`}>
      <div className="key">{name}:</div> <div className="value">{finalValue}</div>
    </li>
  );
};

const InlineBinaryField = ({ id, value, name }: { id: string; value: ConfigurationFieldValue; name: string }) => {
  let finalValue;

  if (value === null || value === undefined || value === '') {
    finalValue = <i>{'<empty>'}</i>;
  } else {
    finalValue = <i>{'<uploaded file content>'}</i>
  }

  return (
    <li key={`${id}-${name}`}>
      <div className="key">{name}:</div> <div className="value">{finalValue}</div>
    </li>
  );
};

const EncryptedField = ({ id, value, name }: { id: string; value: EncryptedFieldValue<unknown>; name: string }) => {
  let finalValue;

  if (!value.is_set) {
    finalValue = <i>{'<empty>'}</i>;
  } else {
    finalValue = PASSWORD_PLACEHOLDER;
  }

  return (
    <li key={`${id}-${name}`}>
      <div className="key">{name}:</div> <div className="value">{finalValue}</div>
    </li>
  );
};

const PasswordField = ({ id, name }: { id: string; name: string }) => (
  <li key={`${id}-${name}`}>
    <div className="key">{name}:</div>
    <div className="value">{PASSWORD_PLACEHOLDER}</div>
  </li>
);

const isPasswordField = (field: ConfigurationField) =>
  field?.type === 'text' && (field.attributes.includes('is_password') || field.attributes.includes('is_sensitive'));

const Configuration = ({
  id: _id,
  config,
  typeDefinition = undefined,
}: {
  id: string;
  config: Props['configuration'];
  typeDefinition?: Props['typeDefinition'];
}) => {
  if (!config) {
    return '';
  }

  const formattedItems = Object.keys(config)
    .sort()
    .map((key) => {
      const value = config[key];
      const requestedConfiguration = typeDefinition?.requested_configuration?.[key];

      if (isPasswordField(requestedConfiguration)) {
        return <PasswordField id={_id} name={key} />;
      }

      if (requestedConfiguration?.type === 'inline_binary') {
        if ('is_encrypted' in requestedConfiguration && requestedConfiguration.is_encrypted) {
          return <EncryptedField id={_id} value={value as EncryptedFieldValue<unknown>} name={key} />;
        }
        return <InlineBinaryField id={_id} value={value} name={key} />;
      }

      return <RegularField id={_id} value={value} name={key} />;
    });

  if (formattedItems.length < 1) {
    formattedItems.push(<li key="placeholder">-- no configuration --</li>);
  }

  return <ul>{formattedItems}</ul>;
};

const StyledWell = styled(Well)(
  ({ theme }) => css`
    margin-top: 5px;
    margin-bottom: 0;
    padding: 9px;
    font-family: ${theme.fonts.family.monospace};
    word-wrap: break-word;

    ul {
      padding: 0;
      margin: 0;
    }

    white-space: pre-line;

    .configuration-section {
      margin-bottom: 10px;
    }

    li:not(:last-child) {
      margin-bottom: 5px;
    }

    .key {
      display: inline;
    }

    .alert-callback .key {
      display: inline-block;
      min-width: 140px;
      vertical-align: top;
    }

    .value {
      display: inline;
    }

    .alert-callback .value {
      display: inline-block;
    }
  `,
);

const ConfigurationWell = ({ id, configuration, typeDefinition = undefined }: Props) => (
  <StyledWell bsSize="small">
    <Configuration id={id} config={configuration} typeDefinition={typeDefinition} />
  </StyledWell>
);

export default ConfigurationWell;
