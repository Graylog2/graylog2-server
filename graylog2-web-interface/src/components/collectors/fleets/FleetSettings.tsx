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
import { TextInput, Textarea, Button, Group, Stack, Card, Text } from '@mantine/core';

import { RelativeTime } from 'components/common';

import type { Fleet } from '../types';

type Props = {
  fleet: Fleet;
  onSave: (updates: Partial<Fleet>) => void;
  isLoading?: boolean;
};

const Section = styled(Card)(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.md};
  `,
);

const SectionTitle = styled.h4(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.sm};
    font-size: ${theme.fonts.size.body};
    font-weight: 600;
  `,
);

const FleetSettings = ({ fleet, onSave, isLoading = false }: Props) => {
  const [name, setName] = useState(fleet.name);
  const [description, setDescription] = useState(fleet.description);
  const [targetVersion, setTargetVersion] = useState(fleet.target_version || '');
  const [isDirty, setIsDirty] = useState(false);

  const handleChange = <T,>(setter: React.Dispatch<React.SetStateAction<T>>, value: T) => {
    setter(value);
    setIsDirty(true);
  };

  const handleSave = () => {
    onSave({
      name,
      description,
      target_version: targetVersion || null,
    });
    setIsDirty(false);
  };

  const handleReset = () => {
    setName(fleet.name);
    setDescription(fleet.description);
    setTargetVersion(fleet.target_version || '');
    setIsDirty(false);
  };

  return (
    <Stack gap="md">
      <Section withBorder>
        <SectionTitle>General Settings</SectionTitle>
        <Stack gap="sm">
          <TextInput
            label="Fleet Name"
            value={name}
            onChange={(e) => handleChange(setName, e.target.value)}
            required
          />
          <Textarea
            label="Description"
            value={description}
            onChange={(e) => handleChange(setDescription, e.target.value)}
          />
          <TextInput
            label="Target Version"
            description="Collector version to deploy to this fleet"
            placeholder="e.g., 1.2.0"
            value={targetVersion}
            onChange={(e) => handleChange(setTargetVersion, e.target.value)}
          />
        </Stack>

        <Group justify="flex-end" mt="md">
          <Button variant="default" onClick={handleReset} disabled={!isDirty || isLoading}>
            Reset
          </Button>
          <Button onClick={handleSave} disabled={!isDirty || !name || isLoading} loading={isLoading}>
            Save Changes
          </Button>
        </Group>
      </Section>

      <Section withBorder>
        <SectionTitle>Fleet Information</SectionTitle>
        <Stack gap="xs">
          <Group>
            <Text size="sm" fw={500} style={{ minWidth: 100 }}>Fleet ID:</Text>
            <Text size="sm" ff="monospace">{fleet.id}</Text>
          </Group>
          <Group>
            <Text size="sm" fw={500} style={{ minWidth: 100 }}>Created:</Text>
            <RelativeTime dateTime={fleet.created_at} />
          </Group>
          <Group>
            <Text size="sm" fw={500} style={{ minWidth: 100 }}>Updated:</Text>
            <RelativeTime dateTime={fleet.updated_at} />
          </Group>
        </Stack>
      </Section>

      <Section withBorder>
        <SectionTitle>Danger Zone</SectionTitle>
        <Text size="sm" c="dimmed" mb="sm">
          Deleting a fleet will remove all configuration. Instances will need to be re-enrolled.
        </Text>
        <Button color="red" variant="outline">Delete Fleet</Button>
      </Section>
    </Stack>
  );
};

export default FleetSettings;
