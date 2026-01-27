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
import { Stack } from '@mantine/core';

import { Button, Input } from 'components/bootstrap';
import { Card, RelativeTime } from 'components/common';

import type { Fleet } from '../types';

type Props = {
  fleet: Fleet;
  onSave: (updates: Partial<Fleet>) => void;
  onDelete?: () => void;
  isLoading?: boolean;
};

const Section = styled(Card)`
  margin-bottom: ${({ theme }) => theme.spacings.md};
`;

const InfoRow = styled.div`
  display: flex;
  align-items: center;
  margin-bottom: ${({ theme }) => theme.spacings.xs};
`;

const InfoLabel = styled.span`
  font-size: ${({ theme }) => theme.fonts.size.small};
  font-weight: 500;
  min-width: 100px;
`;

const InfoValue = styled.span`
  font-size: ${({ theme }) => theme.fonts.size.small};
  font-family: ${({ theme }) => theme.fonts.family.monospace};
`;

const ButtonGroup = styled.div`
  display: flex;
  justify-content: flex-end;
  gap: ${({ theme }) => theme.spacings.sm};
  margin-top: ${({ theme }) => theme.spacings.md};
`;

const WarningText = styled.p`
  font-size: ${({ theme }) => theme.fonts.size.small};
  color: ${({ theme }) => theme.colors.gray[60]};
  margin-bottom: ${({ theme }) => theme.spacings.sm};
`;

const SectionTitle = styled.h4(
  ({ theme }) => css`
    margin-bottom: ${theme.spacings.sm};
    font-size: ${theme.fonts.size.body};
    font-weight: 600;
  `,
);

const FleetSettings = ({ fleet, onSave, onDelete = undefined, isLoading = false }: Props) => {
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
      <Section>
        <SectionTitle>General Settings</SectionTitle>
        <Stack gap="sm">
          <Input
            id="fleet-name"
            label="Fleet Name"
            value={name}
            onChange={(e) => handleChange(setName, e.target.value)}
            required
          />
          <Input
            id="fleet-description"
            type="textarea"
            label="Description"
            value={description}
            onChange={(e) => handleChange(setDescription, e.target.value)}
          />
          <Input
            id="fleet-target-version"
            label="Target Version"
            help="Collector version to deploy to this fleet"
            placeholder="e.g., 1.2.0"
            value={targetVersion}
            onChange={(e) => handleChange(setTargetVersion, e.target.value)}
          />
        </Stack>

        <ButtonGroup>
          <Button bsStyle="default" onClick={handleReset} disabled={!isDirty || isLoading}>
            Reset
          </Button>
          <Button bsStyle="primary" onClick={handleSave} disabled={!isDirty || !name || isLoading}>
            Save Changes
          </Button>
        </ButtonGroup>
      </Section>

      <Section>
        <SectionTitle>Fleet Information</SectionTitle>
        <div>
          <InfoRow>
            <InfoLabel>Fleet ID:</InfoLabel>
            <InfoValue>{fleet.id}</InfoValue>
          </InfoRow>
          <InfoRow>
            <InfoLabel>Created:</InfoLabel>
            <RelativeTime dateTime={fleet.created_at} />
          </InfoRow>
          <InfoRow>
            <InfoLabel>Updated:</InfoLabel>
            <RelativeTime dateTime={fleet.updated_at} />
          </InfoRow>
        </div>
      </Section>

      <Section>
        <SectionTitle>Danger Zone</SectionTitle>
        <WarningText>
          Deleting a fleet will remove all configuration. Instances will need to be re-enrolled.
        </WarningText>
        <Button bsStyle="danger" onClick={onDelete} disabled={!onDelete}>Delete Fleet</Button>
      </Section>
    </Stack>
  );
};

export default FleetSettings;
