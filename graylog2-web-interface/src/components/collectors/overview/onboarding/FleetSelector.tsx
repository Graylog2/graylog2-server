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
import styled, { css } from 'styled-components';

import { Select } from 'components/common';

import type { Fleet } from '../../types';

const CREATE_NEW_VALUE = '__create_new__';

type Props = {
  fleets: Fleet[];
  selectedFleetId: string | null;
  onSelect: (fleetId: string | null, createNew: boolean) => void;
  disabled: boolean;
};

const Container = styled.div(
  ({ theme }) => css`
    max-width: 400px;
    margin: 0 auto ${theme.spacings.md};
    text-align: left;
  `,
);

const Label = styled.label(
  ({ theme }) => css`
    display: block;
    font-weight: 500;
    margin-bottom: ${theme.spacings.xs};
    text-align: center;
  `,
);

const FleetSelector = ({ fleets, selectedFleetId, onSelect, disabled }: Props) => {
  const options = [
    ...fleets.map((f) => ({ label: f.name, value: f.id })),
    { label: 'Create new onboarding fleet', value: CREATE_NEW_VALUE },
  ];

  const handleChange = (value: string) => {
    if (value === CREATE_NEW_VALUE) {
      onSelect(null, true);
    } else {
      onSelect(value, false);
    }
  };

  return (
    <Container>
      <Label htmlFor="onboarding-fleet-select">Choose a fleet for this collector</Label>
      <Select
        inputId="onboarding-fleet-select"
        options={options}
        value={selectedFleetId ?? CREATE_NEW_VALUE}
        onChange={handleChange}
        clearable={false}
        disabled={disabled}
        placeholder="Select a fleet..."
      />
    </Container>
  );
};

export default FleetSelector;
