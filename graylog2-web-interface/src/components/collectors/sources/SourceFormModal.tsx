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
import { useState } from 'react';

import { Input } from 'components/bootstrap';
import Modal from 'components/bootstrap/Modal';
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
  onSave: (source: Omit<Source, 'id'>) => void;
  isLoading?: boolean;
};

const defaultConfigs: Record<SourceType, FileSourceConfig | JournaldSourceConfig | WindowsEventLogSourceConfig> = {
  file: { paths: [''], read_mode: 'end' },
  journald: { read_mode: 'end', priority: 'info' },
  windows_event_log: { channels: [], include_default_channels: true, read_mode: 'end' },
};

const SourceFormModal = ({ fleetId, source = undefined, onClose, onSave, isLoading = false }: Props) => {
  const isEdit = !!source;
  const [sourceType, setSourceType] = useState<SourceType>(source?.type || 'file');
  const [name, setName] = useState(source?.name || '');
  const [description, setDescription] = useState(source?.description || '');
  const [enabled, setEnabled] = useState(source?.enabled ?? true);
  const [config, setConfig] = useState<(typeof defaultConfigs)[SourceType]>(
    source?.config || defaultConfigs[sourceType],
  );

  const handleTypeChange = (type: string) => {
    setSourceType(type as SourceType);
    setConfig(defaultConfigs[type as SourceType]);
  };

  const handleSave = () => {
    onSave({
      fleet_id: fleetId,
      name,
      description,
      enabled,
      type: sourceType,
      config,
    } as Omit<Source, 'id'>);
  };

  const updateFileConfig = (updates: Partial<FileSourceConfig>) => {
    setConfig((prev) => ({ ...prev, ...updates }));
  };

  const updateJournaldConfig = (updates: Partial<JournaldSourceConfig>) => {
    setConfig((prev) => ({ ...prev, ...updates }));
  };
  const updateWindowsEventLogConfig = (updates: Partial<WindowsEventLogSourceConfig>) => {
    setConfig((prev) => ({ ...prev, ...updates }));
  };

  const renderFileConfig = () => {
    const fileConfig = config as FileSourceConfig;

    return (
      <>
        <Input
          id="file-paths"
          type="text"
          label="File Path(s)"
          help="Glob pattern supported (e.g., /var/log/*.log)"
          value={fileConfig.paths[0] || ''}
          onChange={(e) => updateFileConfig({ paths: [e.target.value] })}
          required
        />
        <Input
          id="file-read-mode"
          type="select"
          label="Read Mode"
          value={fileConfig.read_mode}
          onChange={(e) => updateFileConfig({ read_mode: e.target.value as 'beginning' | 'end' })}>
          <option value="end">From end (tail)</option>
          <option value="beginning">From beginning</option>
        </Input>
      </>
    );
  };

  const renderJournaldConfig = () => {
    const journaldConfig = config as JournaldSourceConfig;

    return (
      <>
        <Input
          id="journald-read-mode"
          type="select"
          label="Read Mode"
          value={journaldConfig.read_mode}
          onChange={(e) => updateJournaldConfig({ read_mode: e.target.value as 'beginning' | 'end' })}>
          <option value="end">From end (tail)</option>
          <option value="beginning">From beginning</option>
        </Input>
        <Input
          id="journald-priority"
          type="select"
          label="Priority"
          value={journaldConfig.priority}
          onChange={(e) => updateJournaldConfig({ priority: e.target.value as JournaldPriority })}>
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
          value={journaldConfig.match_pattern || ''}
          onChange={(e) => updateJournaldConfig({ match_pattern: e.target.value || undefined })}
        />
      </>
    );
  };

  const renderWindowsEventLogConfig = () => {
    const winConfig = config as WindowsEventLogSourceConfig;

    return (
      <>
        <Input
          id="win-channels"
          type="text"
          label="Channels"
          help="Comma-separated channel names (e.g., Application, Security, System)"
          value={winConfig.channels.join(', ')}
          onChange={(e) =>
            updateWindowsEventLogConfig({
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
          checked={winConfig.include_default_channels}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) =>
            updateWindowsEventLogConfig({ include_default_channels: e.target.checked })
          }
        />
        <Input
          id="win-read-mode"
          type="select"
          label="Read Mode"
          value={winConfig.read_mode}
          onChange={(e) => updateWindowsEventLogConfig({ read_mode: e.target.value as 'beginning' | 'end' })}>
          <option value="end">From end (tail)</option>
          <option value="beginning">From beginning</option>
        </Input>
      </>
    );
  };

  const renderConfigSection = () => {
    switch (sourceType) {
      case 'file':
        return renderFileConfig();
      case 'journald':
        return renderJournaldConfig();
      case 'windows_event_log':
        return renderWindowsEventLogConfig();
      default:
        return null;
    }
  };

  return (
    <Modal show onHide={onClose} bsSize="lg">
      <Modal.Header>
        <Modal.Title>{isEdit ? 'Edit Source' : 'New Source'}</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <Input
          id="source-type"
          type="select"
          label="Source Type"
          value={sourceType}
          onChange={(e) => handleTypeChange(e.target.value)}
          disabled={isEdit}>
          {Object.entries(SOURCE_TYPE_LABELS).map(([value, label]) => (
            <option key={value} value={value}>{label}</option>
          ))}
        </Input>
        <Input
          id="source-name"
          type="text"
          label="Name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
        />
        <Input
          id="source-description"
          type="textarea"
          label="Description"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
        <Input
          id="source-enabled"
          type="checkbox"
          label="Enabled"
          checked={enabled}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) => setEnabled(e.target.checked)}
        />
        {renderConfigSection()}
      </Modal.Body>
      <Modal.Footer>
        <ModalSubmit
          isAsyncSubmit
          submitButtonText={`${isEdit ? 'Update' : 'Create'} ${name}`}
          submitLoadingText={`${isEdit ? 'Updating...' : 'Creating...'}`}
          onCancel={onClose}
          onSubmit={handleSave}
          disabledSubmit={!name || isLoading}
          isSubmitting={isLoading}
        />
      </Modal.Footer>
    </Modal>
  );
};

export default SourceFormModal;
