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
import styled, { css } from 'styled-components';
import {
  SegmentedControl,
  TextInput,
  Textarea,
  Group,
  Stack,
  Button,
  NumberInput,
  Text,
} from '@mantine/core';

import { Input } from 'components/bootstrap';
import Modal from 'components/bootstrap/Modal';

import type { Source, SourceType, FileSourceConfig, JournaldSourceConfig, TcpSourceConfig, UdpSourceConfig, WindowsEventLogSourceConfig } from '../types';

type Props = {
  fleetId: string;
  source?: Source;
  onClose: () => void;
  onSave: (source: Omit<Source, 'id'>) => void;
  isLoading?: boolean;
};

const FormSection = styled.div(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.md};
  `,
);

const sourceTypeLabels: Record<SourceType, string> = {
  file: 'File',
  journald: 'Journald',
  windows_event_log: 'WinEventLog',
  tcp: 'TCP',
  udp: 'UDP',
};

const defaultConfigs: Record<SourceType, FileSourceConfig | JournaldSourceConfig | TcpSourceConfig | UdpSourceConfig | WindowsEventLogSourceConfig> = {
  file: { paths: [''], read_mode: 'end' },
  journald: { units: [], priority: 6 },
  windows_event_log: { channels: ['Application'], read_mode: 'end', event_format: 'json' },
  tcp: { bind_address: '0.0.0.0', port: 5514, framing: 'newline' },
  udp: { bind_address: '0.0.0.0', port: 5514 },
};

const SourceFormModal = ({ fleetId, source, onClose, onSave, isLoading = false }: Props) => {
  const isEdit = !!source;
  const [sourceType, setSourceType] = useState<SourceType>(source?.type || 'file');
  const [name, setName] = useState(source?.name || '');
  const [description, setDescription] = useState(source?.description || '');
  const [enabled, setEnabled] = useState(source?.enabled ?? true);
  const [config, setConfig] = useState<typeof defaultConfigs[SourceType]>(
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

  const updateTcpConfig = (updates: Partial<TcpSourceConfig>) => {
    setConfig((prev) => ({ ...prev, ...updates }));
  };

  const updateUdpConfig = (updates: Partial<UdpSourceConfig>) => {
    setConfig((prev) => ({ ...prev, ...updates }));
  };

  const updateWindowsEventLogConfig = (updates: Partial<WindowsEventLogSourceConfig>) => {
    setConfig((prev) => ({ ...prev, ...updates }));
  };

  const renderFileConfig = () => {
    const fileConfig = config as FileSourceConfig;

    return (
      <>
        <FormSection>
          <TextInput
            label="File Path(s)"
            description="Glob pattern supported (e.g., /var/log/*.log)"
            value={fileConfig.paths[0] || ''}
            onChange={(e) => updateFileConfig({ paths: [e.target.value] })}
            required
          />
        </FormSection>
        <FormSection>
          <Text size="sm" fw={500} mb="xs">Read Mode</Text>
          <SegmentedControl
            value={fileConfig.read_mode}
            onChange={(v) => updateFileConfig({ read_mode: v as 'beginning' | 'end' })}
            data={[
              { value: 'end', label: 'From end (tail)' },
              { value: 'beginning', label: 'From beginning' },
            ]}
          />
        </FormSection>
      </>
    );
  };

  const renderJournaldConfig = () => {
    const journaldConfig = config as JournaldSourceConfig;

    return (
      <>
        <FormSection>
          <TextInput
            label="Units"
            description="Comma-separated systemd unit names (leave empty for all)"
            value={journaldConfig.units.join(', ')}
            onChange={(e) => updateJournaldConfig({ units: e.target.value.split(',').map((u) => u.trim()).filter(Boolean) })}
          />
        </FormSection>
        <FormSection>
          <NumberInput
            label="Priority"
            description="Log priority level (0-7)"
            value={journaldConfig.priority}
            onChange={(v) => updateJournaldConfig({ priority: Number(v) || 6 })}
            min={0}
            max={7}
          />
        </FormSection>
      </>
    );
  };

  const renderTcpConfig = () => {
    const tcpConfig = config as TcpSourceConfig;

    return (
      <>
        <FormSection>
          <TextInput
            label="Bind Address"
            value={tcpConfig.bind_address}
            onChange={(e) => updateTcpConfig({ bind_address: e.target.value })}
            required
          />
        </FormSection>
        <FormSection>
          <NumberInput
            label="Port"
            value={tcpConfig.port}
            onChange={(v) => updateTcpConfig({ port: Number(v) || 5514 })}
            min={1}
            max={65535}
            required
          />
        </FormSection>
        <FormSection>
          <Text size="sm" fw={500} mb="xs">Framing</Text>
          <SegmentedControl
            value={tcpConfig.framing}
            onChange={(v) => updateTcpConfig({ framing: v as 'newline' | 'octet_counting' })}
            data={[
              { value: 'newline', label: 'Newline' },
              { value: 'octet_counting', label: 'Octet Counting' },
            ]}
          />
        </FormSection>
      </>
    );
  };

  const renderUdpConfig = () => {
    const udpConfig = config as UdpSourceConfig;

    return (
      <>
        <FormSection>
          <TextInput
            label="Bind Address"
            value={udpConfig.bind_address}
            onChange={(e) => updateUdpConfig({ bind_address: e.target.value })}
            required
          />
        </FormSection>
        <FormSection>
          <NumberInput
            label="Port"
            value={udpConfig.port}
            onChange={(v) => updateUdpConfig({ port: Number(v) || 5514 })}
            min={1}
            max={65535}
            required
          />
        </FormSection>
      </>
    );
  };

  const renderWindowsEventLogConfig = () => {
    const winConfig = config as WindowsEventLogSourceConfig;

    return (
      <>
        <FormSection>
          <TextInput
            label="Channels"
            description="Comma-separated channel names (e.g., Security, Application)"
            value={winConfig.channels.join(', ')}
            onChange={(e) => updateWindowsEventLogConfig({ channels: e.target.value.split(',').map((c) => c.trim()).filter(Boolean) })}
            required
          />
        </FormSection>
        <FormSection>
          <Text size="sm" fw={500} mb="xs">Read Mode</Text>
          <SegmentedControl
            value={winConfig.read_mode}
            onChange={(v) => updateWindowsEventLogConfig({ read_mode: v as 'beginning' | 'end' })}
            data={[
              { value: 'end', label: 'From end (tail)' },
              { value: 'beginning', label: 'From beginning' },
            ]}
          />
        </FormSection>
        <FormSection>
          <Text size="sm" fw={500} mb="xs">Event Format</Text>
          <SegmentedControl
            value={winConfig.event_format}
            onChange={(v) => updateWindowsEventLogConfig({ event_format: v as 'json' | 'xml' })}
            data={[
              { value: 'json', label: 'JSON' },
              { value: 'xml', label: 'XML' },
            ]}
          />
        </FormSection>
      </>
    );
  };

  const renderConfigSection = () => {
    switch (sourceType) {
      case 'file':
        return renderFileConfig();
      case 'journald':
        return renderJournaldConfig();
      case 'tcp':
        return renderTcpConfig();
      case 'udp':
        return renderUdpConfig();
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
        <Stack gap="md">
          <FormSection>
            <SegmentedControl
              value={sourceType}
              onChange={handleTypeChange}
              data={Object.entries(sourceTypeLabels).map(([value, label]) => ({ value, label }))}
              disabled={isEdit}
              fullWidth
            />
          </FormSection>

          <FormSection>
            <TextInput
              label="Name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              required
            />
          </FormSection>

          <FormSection>
            <Textarea
              label="Description"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </FormSection>

          <FormSection>
            <Input
              id="source-enabled"
              type="checkbox"
              label="Enabled"
              checked={enabled}
              onChange={(e: React.ChangeEvent<HTMLInputElement>) => setEnabled(e.target.checked)}
            />
          </FormSection>

          {renderConfigSection()}
        </Stack>
      </Modal.Body>
      <Modal.Footer>
        <Group justify="flex-end">
          <Button variant="default" onClick={onClose}>Cancel</Button>
          <Button onClick={handleSave} disabled={!name || isLoading} loading={isLoading}>Save Source</Button>
        </Group>
      </Modal.Footer>
    </Modal>
  );
};

export default SourceFormModal;
