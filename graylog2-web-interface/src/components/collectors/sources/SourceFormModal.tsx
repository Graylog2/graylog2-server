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

import { Button, Input, SegmentedControl } from 'components/bootstrap';
import Modal from 'components/bootstrap/Modal';

import type {
  Source,
  SourceType,
  FileSourceConfig,
  JournaldSourceConfig,
  WindowsEventLogSourceConfig,
  MacOSUnifiedLoggingSourceConfig,
  JournaldPriority
} from '../types';

type Props = {
  fleetId: string;
  source?: Source;
  onClose: () => void;
  onSave: (source: Omit<Source, 'id'>) => void;
  isLoading?: boolean;
};

const sourceTypeLabels: Record<SourceType, string> = {
  file: 'File',
  journald: 'Journald',
  windows_event_log: 'Windows Event Log',
  macos_unified_logging: 'macOS Unified Log',
};

const defaultConfigs: Record<
  SourceType,
  FileSourceConfig | JournaldSourceConfig | WindowsEventLogSourceConfig | MacOSUnifiedLoggingSourceConfig
> = {
  file: { paths: [''], read_mode: 'end' },
  journald: { read_mode: 'end', priority: 'info' },
  windows_event_log: { channels: [], include_default_channels: true, read_mode: 'end',  },
  macos_unified_logging: {},
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
  const updateMacOSUnifiedLoggingConfig = (updates: Partial<MacOSUnifiedLoggingSourceConfig>) => {
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
        <div>
          <label htmlFor="file-read-mode">Read Mode</label>
          <SegmentedControl
            value={fileConfig.read_mode}
            onChange={(v) => updateFileConfig({ read_mode: v as 'beginning' | 'end' })}
            data={[
              { value: 'end', label: 'From end (tail)' },
              { value: 'beginning', label: 'From beginning' },
            ]}
          />
        </div>
      </>
    );
  };

  const renderJournaldConfig = () => {
    const journaldConfig = config as JournaldSourceConfig;

    return (
      <>
        <div>
          <label htmlFor="file-read-mode">Read Mode</label>
          <SegmentedControl
            value={journaldConfig.read_mode}
            onChange={(v) => updateJournaldConfig({ read_mode: v as 'beginning' | 'end' })}
            data={[
              { value: 'end', label: 'From end (tail)' },
              { value: 'beginning', label: 'From beginning' },
            ]}
          />
        </div>
        <div>
          <label htmlFor="journald-priority">Priority</label>
          <SegmentedControl
            value={journaldConfig.priority}
            onChange={(v) => updateJournaldConfig({ priority: v as JournaldPriority })}
            data={[
              { value: 'emerg', label: 'EMERG' },
              { value: 'alert', label: 'ALERT' },
              { value: 'crit', label: 'CRIT' },
              { value: 'err', label: 'ERR' },
              { value: 'warning', label: 'WARNING' },
              { value: 'notice', label: 'NOTICE' },
              { value: 'info', label: 'INFO' },
              { value: 'debug', label: 'DEBUG' },
            ]}
          />
        </div>
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
          onChange={(e: React.ChangeEvent<HTMLInputElement>) => updateWindowsEventLogConfig({ include_default_channels: e.target.checked})}
        />
        <div>
          <label htmlFor="win-read-mode">Read Mode</label>
          <SegmentedControl
            value={winConfig.read_mode}
            onChange={(v) => updateWindowsEventLogConfig({ read_mode: v as 'beginning' | 'end' })}
            data={[
              { value: 'end', label: 'From end (tail)' },
              { value: 'beginning', label: 'From beginning' },
            ]}
          />
        </div>
      </>
    );
  };

  const renderMacOSUnifiedLoggingConfig = () => {
    const macConfig = config as MacOSUnifiedLoggingSourceConfig;

    return (
      <>
        <Input
          id="macos-predicate"
          type="text"
          label="Predicate"
          help='Optional NSPredicate filter expression (e.g., process == "kernel")'
          value={macConfig.predicate || ''}
          onChange={(e) => updateMacOSUnifiedLoggingConfig({ predicate: e.target.value || undefined })}
        />
        <div>
          <label htmlFor="macos-format">Format</label>
          <SegmentedControl
            value={macConfig.format || 'ndjson'}
            onChange={(v) =>
              updateMacOSUnifiedLoggingConfig({ format: v as MacOSUnifiedLoggingSourceConfig['format'] })
            }
            data={[
              { value: 'ndjson', label: 'NDJSON' },
              { value: 'json', label: 'JSON' },
              { value: 'default', label: 'Default' },
              { value: 'syslog', label: 'Syslog' },
              { value: 'compact', label: 'Compact' },
            ]}
          />
        </div>
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
      case 'macos_unified_logging':
        return renderMacOSUnifiedLoggingConfig();
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
        <div>
          <label htmlFor="source-type">Source Type</label>
          <SegmentedControl
            value={sourceType}
            onChange={handleTypeChange}
            data={Object.entries(sourceTypeLabels).map(([value, label]) => ({ value, label }))}
            disabled={isEdit}
          />
        </div>
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
        <Button bsStyle="default" onClick={onClose}>
          Cancel
        </Button>{' '}
        <Button bsStyle="primary" onClick={handleSave} disabled={!name || isLoading}>
          Save Source
        </Button>
      </Modal.Footer>
    </Modal>
  );
};

export default SourceFormModal;
