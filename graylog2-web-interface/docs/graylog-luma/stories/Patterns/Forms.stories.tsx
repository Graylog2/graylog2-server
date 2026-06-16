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

// ── Form validation ───────────────────────────────────────────────────────────
const validate = (values: FormValues) => {
  const errors: Partial<FormValues> = {};

  if (!values.name.trim()) {
    errors.name = 'Name is required. Enter a name for this input.';
  } else if (values.name.length > 100) {
    errors.name = 'Name must be 100 characters or fewer.';
  }

  if (!values.hostname.trim()) errors.hostname = 'Hostname is required.';

  if (!values.port.trim()) {
    errors.port = 'Port is required.';
  } else if (!/^\d+$/.test(values.port) || Number(values.port) < 1 || Number(values.port) > 65535) {
    errors.port = 'Port must be a number between 1 and 65535.';
  }

  if (!values.username.trim()) errors.username = 'Username is required.';

  if (!values.indexSet.trim()) errors.indexSet = 'Index set is required. Specify which index set to write to.';

  return errors;
};

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
      validate={validate}
      // eslint-disable-next-line no-alert
      onSubmit={(values) => window.alert(`Input "${values.name}" created.`)}>
      <Row className="content">
        <Col md={8}>
          <Form noValidate>
            {/* ── Group 1: Identity ─────────────────────────────── */}
            <h3 style={{ marginTop: 0 }}>Identity</h3>
            <hr />

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
                  placeholder="e.g. 5140"
                />
              </Col>
            </Row>

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
