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
import { useCallback } from 'react';
import { Formik, Form } from 'formik';

import { Input } from 'components/bootstrap';
import Modal from 'components/bootstrap/Modal';
import { FormikInput } from 'components/common';
import ModalSubmit from 'components/common/ModalSubmit';

import { SOURCE_TYPE_LABELS } from './Constants';

import type {
  Source,
  SourceType,
  FileSourceConfig,
  JournaldSourceConfig,
  JournaldPriority,
  WindowsEventLogSourceConfig,
} from '../types';

type Props = {
  fleetId: string;
  source?: Source;
  onClose: () => void;
  onSave: (source: Omit<Source, 'id'>) => Promise<void>;
};

const defaultConfigs: Record<SourceType, FileSourceConfig | JournaldSourceConfig | WindowsEventLogSourceConfig> = {
  file: { paths: [''], read_mode: 'end' },
  journald: { read_mode: 'end', priority: 'info' },
  windows_event_log: { channels: [], include_default_channels: true, read_mode: 'end' },
};

type FormValues = {
  source_type: SourceType;
  name: string;
  description: string;
  enabled: boolean;
  config: FileSourceConfig | JournaldSourceConfig | WindowsEventLogSourceConfig;
};

const validate = (values: FormValues) => {
  const errors: Partial<Record<keyof FormValues, string>> = {};

  if (!values.name) {
    errors.name = 'Name is required';
  }

  return errors;
};

const FileConfigFields = ({
  config,
  setFieldValue,
}: {
  config: FileSourceConfig;
  setFieldValue: (field: string, value: unknown) => void;
}) => (
  <>
    <Input
      id="file-paths"
      type="text"
      label="File Path(s)"
      help="Glob pattern supported (e.g., /var/log/*.log)"
      value={config.paths[0] || ''}
      onChange={(e) => setFieldValue('config', { ...config, paths: [e.target.value] })}
      required
    />
    <Input
      id="file-read-mode"
      type="select"
      label="Read Mode"
      value={config.read_mode}
      onChange={(e) => setFieldValue('config', { ...config, read_mode: e.target.value as 'beginning' | 'end' })}>
      <option value="end">From end (tail)</option>
      <option value="beginning">From beginning</option>
    </Input>
  </>
);

const JournaldConfigFields = ({
  config,
  setFieldValue,
}: {
  config: JournaldSourceConfig;
  setFieldValue: (field: string, value: unknown) => void;
}) => (
  <>
    <Input
      id="journald-read-mode"
      type="select"
      label="Read Mode"
      value={config.read_mode}
      onChange={(e) => setFieldValue('config', { ...config, read_mode: e.target.value as 'beginning' | 'end' })}>
      <option value="end">From end (tail)</option>
      <option value="beginning">From beginning</option>
    </Input>
    <Input
      id="journald-priority"
      type="select"
      label="Priority"
      value={config.priority}
      onChange={(e) => setFieldValue('config', { ...config, priority: e.target.value as JournaldPriority })}>
      <option value="emerg">Emergency</option>
      <option value="alert">Alert</option>
      <option value="crit">Critical</option>
      <option value="err">Error</option>
      <option value="warning">Warning</option>
      <option value="notice">Notice</option>
      <option value="info">Info</option>
      <option value="debug">Debug</option>
    </Input>
    <Input
      id="journald-match-pattern"
      type="text"
      label="Match Pattern"
      help="Optional journald match expression to filter entries"
      value={config.match_pattern || ''}
      onChange={(e) => setFieldValue('config', { ...config, match_pattern: e.target.value || undefined })}
    />
  </>
);

const WindowsEventLogConfigFields = ({
  config,
  setFieldValue,
}: {
  config: WindowsEventLogSourceConfig;
  setFieldValue: (field: string, value: unknown) => void;
}) => (
  <>
    <Input
      id="win-channels"
      type="text"
      label="Channels"
      help="Comma-separated channel names (e.g., Application, Security, System)"
      value={config.channels.join(', ')}
      onChange={(e) =>
        setFieldValue('config', {
          ...config,
          channels: e.target.value
            .split(',')
            .map((c) => c.trim())
            .filter(Boolean),
        })
      }
      required
    />
    <Input
      id="win-include-default-channels"
      type="checkbox"
      label="Include default channels"
      checked={config.include_default_channels}
      onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
        setFieldValue('config', { ...config, include_default_channels: e.target.checked })
      }
    />
    <Input
      id="win-read-mode"
      type="select"
      label="Read Mode"
      value={config.read_mode}
      onChange={(e) => setFieldValue('config', { ...config, read_mode: e.target.value as 'beginning' | 'end' })}>
      <option value="end">From end (tail)</option>
      <option value="beginning">From beginning</option>
    </Input>
  </>
);

const SourceFormModal = ({ fleetId, source = undefined, onClose, onSave }: Props) => {
  const isEdit = !!source;

  const initialValues: FormValues = {
    source_type: source?.type || 'file',
    name: source?.name || '',
    description: source?.description || '',
    enabled: source?.enabled ?? true,
    config: source?.config || defaultConfigs[source?.type || 'file'],
  };

  const handleSubmit = useCallback(
    (values: FormValues) =>
      onSave({
        fleet_id: fleetId,
        name: values.name,
        description: values.description,
        enabled: values.enabled,
        type: values.source_type,
        config: values.config,
      } as Omit<Source, 'id'>).then(() => onClose()),
    [fleetId, onSave, onClose],
  );

  return (
    <Modal show onHide={onClose} bsSize="lg">
      <Formik<FormValues> initialValues={initialValues} onSubmit={handleSubmit} validate={validate}>
        {({ isSubmitting, isValidating, values, setFieldValue }) => (
          <Form>
            <Modal.Header>
              <Modal.Title>{isEdit ? 'Edit Source' : 'New Source'}</Modal.Title>
            </Modal.Header>
            <Modal.Body>
              <Input
                id="source-type"
                type="select"
                label="Source Type"
                value={values.source_type}
                onChange={(e) => {
                  const newType = e.target.value as SourceType;

                  setFieldValue('source_type', newType);
                  setFieldValue('config', defaultConfigs[newType]);
                }}
                disabled={isEdit}>
                {Object.entries(SOURCE_TYPE_LABELS).map(([value, label]) => (
                  <option key={value} value={value}>
                    {label}
                  </option>
                ))}
              </Input>
              <FormikInput id="source-name" label="Name" name="name" required />
              <FormikInput id="source-description" label="Description" name="description" type="textarea" />
              <FormikInput id="source-enabled" label="Enabled" name="enabled" type="checkbox" />
              {values.source_type === 'file' && (
                <FileConfigFields config={values.config as FileSourceConfig} setFieldValue={setFieldValue} />
              )}
              {values.source_type === 'journald' && (
                <JournaldConfigFields config={values.config as JournaldSourceConfig} setFieldValue={setFieldValue} />
              )}
              {values.source_type === 'windows_event_log' && (
                <WindowsEventLogConfigFields
                  config={values.config as WindowsEventLogSourceConfig}
                  setFieldValue={setFieldValue}
                />
              )}
            </Modal.Body>
            <Modal.Footer>
              <ModalSubmit
                submitButtonText={isEdit ? 'Update source' : 'Create source'}
                submitLoadingText={isEdit ? 'Updating...' : 'Creating...'}
                onCancel={onClose}
                disabledSubmit={isValidating}
                isSubmitting={isSubmitting}
              />
            </Modal.Footer>
          </Form>
        )}
      </Formik>
    </Modal>
  );
};

export default SourceFormModal;
