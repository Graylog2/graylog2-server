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
import type { Meta, StoryObj } from '@storybook/react-webpack5';
import { Formik, Form } from 'formik';

import { Col, Row } from 'components/bootstrap';
import Collapsible from 'components/common/Collapsible';
import FormikInput from 'components/common/FormikInput';
import FormSubmit from 'components/common/FormSubmit';
import RequiredMarker from 'components/common/RequiredMarker';

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

// ── Stories ───────────────────────────────────────────────────────────────────

export const FormPatternExample: StoryObj = {
  tags: ['!dev'],
  parameters: {
    docs: {
      source: { type: 'dynamic' },
    },
  },
  render: () => (
    <Formik
        initialValues={INITIAL}
        // eslint-disable-next-line no-alert
        onSubmit={(values) => window.alert(`Input "${values.name}" created.`)}>
        <Row className="content">
          <Col md={8}>
            <Form noValidate>
              <h2>Create Input</h2>

              {/* ── Group 1: Identity ─────────────────────────────── */}
              <h3 style={{ marginTop: 0 }}>Identity</h3>
              <hr />

              <FormikInput
                id="name"
                name="name"
                label={<>Name<RequiredMarker /></>}
                required
                validate={nameValidator}
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
                    label={<>Hostname<RequiredMarker /></>}
                    required
                    validate={requiredValidator('Hostname')}
                    placeholder="e.g. 192.168.1.1"
                  />
                </Col>
                <Col sm={6}>
                  <FormikInput
                    id="port"
                    name="port"
                    label={<>Port<RequiredMarker /></>}
                    required
                    validate={portValidator}
                    placeholder="e.g. 5140"
                  />
                </Col>
              </Row>

              <FormikInput
                id="username"
                name="username"
                label={<>Username<RequiredMarker /></>}
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
                label={<>Index set<RequiredMarker /></>}
                required
                validate={indexSetValidator}
                placeholder="e.g. graylog"
              />

              {/* ── Advanced options (hidden by default) ──────────── */}
              <Collapsible label="Advanced options">
                <FormikInput id="timeout" name="timeout" label="Connection timeout (ms)" placeholder="e.g. 5000" />
                <FormikInput id="batchSize" name="batchSize" label="Max batch size" placeholder="e.g. 1000" />
              </Collapsible>

              <FormSubmit submitButtonText="Create Input" displayCancel={false} />
            </Form>
          </Col>
        </Row>
      </Formik>
  ),
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
