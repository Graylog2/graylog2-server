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
import React, { useCallback, useRef, useState } from 'react';
import type { Meta, StoryObj } from '@storybook/react-webpack5';
import styled from 'styled-components';
import { Formik, Form } from 'formik';
import type { FormikHelpers } from 'formik';

import { Button, Col, Row } from 'components/bootstrap';
import FormikInput from 'components/common/FormikInput';
import FormSubmit from 'components/common/FormSubmit';
import RequiredMarker from 'components/common/RequiredMarker';

const AdvancedToggle = styled.button`
  background: none;
  border: none;
  padding: ${({ theme }) => theme.spacings.xxs} 0;
  margin-bottom: ${({ theme }) => theme.spacings.md};
  cursor: pointer;
  font-size: inherit;
  color: inherit;
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacings.xs};
`;

// ── Types ─────────────────────────────────────────────────────────────────────

type FormValues = {
  name: string;
  description: string;
  hostname: string;
  port: string;
  username: string;
  password: string;
  indexSet: string;
  timeout: string;
  batchSize: string;
};

const INITIAL: FormValues = {
  name: '',
  description: '',
  hostname: '',
  port: '',
  username: '',
  password: '',
  indexSet: '',
  timeout: '',
  batchSize: '',
};

// ── Sync field validators ─────────────────────────────────────────────────────

const requiredValidator = (label: string) => (value: string) => (value.trim() ? undefined : `${label} is required.`);

const nameValidator = (value: string) => {
  if (!value.trim()) return 'Name is required. Enter a name for this input.';
  if (value.length > 100) return 'Name must be 100 characters or fewer.';

  return undefined;
};

const portValidator = (value: string) => {
  if (!value.trim()) return 'Port is required.';
  if (!/^\d+$/.test(value) || Number(value) < 1 || Number(value) > 65535)
    return 'Port must be a number between 1 and 65535.';

  return undefined;
};

const indexSetValidator = (value: string) =>
  value.trim() ? undefined : 'Index set is required. Specify which index set to write to.';

// ── Form component ────────────────────────────────────────────────────────────

const FormExample: React.FC = () => {
  // Async "name already taken" check. Both state (for re-render) and ref (so
  // onSubmit can read the latest value without a stale closure).
  const asyncNameErrorRef = useRef<string | undefined>(undefined);
  const [asyncNameError, setAsyncNameError] = useState<string | undefined>();
  const [showAdvanced, setShowAdvanced] = useState(false);
  const [submittedName, setSubmittedName] = useState<string | undefined>();
  const debounceRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Debounce 300ms after typing stops; clear immediately on any new keystroke.
  const handleNameChange = useCallback((e: React.ChangeEvent<HTMLInputElement>) => {
    const { value } = e.target;
    asyncNameErrorRef.current = undefined;
    setAsyncNameError(undefined);
    if (debounceRef.current) clearTimeout(debounceRef.current);

    debounceRef.current = setTimeout(() => {
      // Simulate an async "name already taken" check — try "graylog-default".
      if (value.trim().toLowerCase() === 'graylog-default') {
        const err = '"graylog-default" is already in use. Choose a different name.';
        asyncNameErrorRef.current = err;
        setAsyncNameError(err);
      }
    }, 300);
  }, []);

  const handleSubmit = useCallback((values: FormValues, { setFieldError }: FormikHelpers<FormValues>) => {
    // Async error is not part of Formik's field validate, so guard here too.
    if (asyncNameErrorRef.current) {
      setFieldError('name', asyncNameErrorRef.current);

      return;
    }

    setSubmittedName(values.name);
  }, []);

  if (submittedName !== undefined) {
    return (
      <div>
        <p>
          <strong>Input &ldquo;{submittedName}&rdquo; created.</strong>
        </p>
        <Button
          onClick={() => {
            asyncNameErrorRef.current = undefined;
            setAsyncNameError(undefined);
            setSubmittedName(undefined);
          }}>
          Reset
        </Button>
      </div>
    );
  }

  return (
    <Formik initialValues={INITIAL} onSubmit={handleSubmit}>
      <Row className="content">
        <Col md={8}>
          <Form noValidate>
            {/* ── Group 1: Identity ─────────────────────────────── */}
            <h3 style={{ marginTop: 0 }}>Identity</h3>
            <hr />

            {/* Required before optional within the group */}
            <FormikInput
              id="name"
              name="name"
              label={
                <>
                  Name
                  <RequiredMarker />
                </>
              }
              required
              validate={nameValidator}
              onChange={handleNameChange}
              error={asyncNameError}
              placeholder="e.g. production-syslog"
            />
            <FormikInput
              id="description"
              name="description"
              label="Description"
              placeholder="What does this input collect?"
            />

            {/* ── Group 2: Connection ───────────────────────────── */}
            <h3>Connection</h3>
            <hr />

            {/* Paired fields: semantically one unit, equal width */}
            <Row>
              <Col sm={6}>
                <FormikInput
                  id="hostname"
                  name="hostname"
                  label={
                    <>
                      Hostname
                      <RequiredMarker />
                    </>
                  }
                  required
                  validate={requiredValidator('Hostname')}
                  placeholder="e.g. 192.168.1.1"
                />
              </Col>
              <Col sm={6}>
                <FormikInput
                  id="port"
                  name="port"
                  label={
                    <>
                      Port
                      <RequiredMarker />
                    </>
                  }
                  required
                  validate={portValidator}
                  placeholder="e.g. 5140"
                />
              </Col>
            </Row>

            {/* Required before optional within the group */}
            <FormikInput
              id="username"
              name="username"
              label={
                <>
                  Username
                  <RequiredMarker />
                </>
              }
              required
              validate={requiredValidator('Username')}
            />
            <FormikInput id="password" name="password" type="password" label="Password" />

            {/* ── Group 3: Output ───────────────────────────────── */}
            <h3>Output</h3>
            <hr />

            <FormikInput
              id="indexSet"
              name="indexSet"
              label={
                <>
                  Index set
                  <RequiredMarker />
                </>
              }
              required
              validate={indexSetValidator}
              placeholder="e.g. graylog"
            />

            {/* ── Advanced options (hidden by default) ──────────── */}
            <AdvancedToggle type="button" onClick={() => setShowAdvanced((v) => !v)} aria-expanded={showAdvanced}>
              {showAdvanced ? '▾' : '▸'} Advanced options
            </AdvancedToggle>

            {showAdvanced && (
              <>
                <FormikInput id="timeout" name="timeout" label="Connection timeout (ms)" placeholder="e.g. 5000" />
                <FormikInput id="batchSize" name="batchSize" label="Max batch size" placeholder="e.g. 1000" />
              </>
            )}

            <FormSubmit submitButtonText="Create Input" displayCancel={false} />
          </Form>
        </Col>
      </Row>
    </Formik>
  );
};

// ── Stories ───────────────────────────────────────────────────────────────────

export const FormPatternExample: StoryObj = {
  tags: ['!dev'],
  parameters: {
    docs: {
      source: { type: 'dynamic' },
    },
  },
  render: () => <FormExample />,
};

// TODO: Replace these focused stubs with real interactive examples.
//   - PairedFields    — isolated paired-field row (e.g. key / value at equal width)
//   - RequiredFields  — small form highlighting the asterisk (*) convention
//   - FieldValidation — single field cycling untouched → blur error → on-input recovery

export const PairedFields: StoryObj = {
  tags: ['!dev'],
  render: () => <div />,
};

export const RequiredFields: StoryObj = {
  tags: ['!dev'],
  render: () => <div />,
};

export const FieldValidation: StoryObj = {
  tags: ['!dev'],
  render: () => <div />,
};

const meta: Meta = {
  title: 'Patterns/Forms',
  parameters: { layout: 'padded' },
};

export default meta;
