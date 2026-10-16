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
import { Field } from 'formik';

import { Button, Input } from 'components/bootstrap';

import type { RuntimeTunableFieldProps, SecretValue } from '../types';

const SecretField = ({ name, label, help, disabled = false }: RuntimeTunableFieldProps) => (
  <Field name={name}>
    {({ field: { value }, form: { initialValues, setFieldValue } }) => {
      const secret = value as SecretValue;
      const wasSet = 'keep_value' in (initialValues[name] ?? {});
      const undo = wasSet ? (
        <Button onClick={() => setFieldValue(name, { keep_value: true })} disabled={disabled}>
          Undo
        </Button>
      ) : undefined;

      if ('keep_value' in secret) {
        return (
          <Input id={name} label={label} help={help}>
            <span>Configured </span>
            <Button
              bsSize="xs"
              aria-label={`Change ${label}`}
              onClick={() => setFieldValue(name, { set_value: '' })}
              disabled={disabled}>
              Change
            </Button>{' '}
            <Button
              bsSize="xs"
              bsStyle="danger"
              aria-label={`Remove ${label}`}
              onClick={() => setFieldValue(name, { delete_value: true })}
              disabled={disabled}>
              Remove
            </Button>
          </Input>
        );
      }
      if ('delete_value' in secret) {
        return (
          <Input id={name} label={label} help="Will be removed when saving." buttonAfter={undo}>
            <span>Removed</span>
          </Input>
        );
      }

      return (
        <Input
          id={name}
          type="password"
          label={label}
          help={help}
          autoComplete="new-password"
          value={'set_value' in secret ? secret.set_value : ''}
          disabled={disabled}
          buttonAfter={undo}
          onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
            setFieldValue(name, { set_value: event.target.value })
          }
        />
      );
    }}
  </Field>
);

export default SecretField;
