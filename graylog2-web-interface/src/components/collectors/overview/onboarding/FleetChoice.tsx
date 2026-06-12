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

import { Button } from 'components/bootstrap';
import { Select } from 'components/common';

import type { Fleet } from '../../types';

// A resolved decision about which fleet the collector should enroll into.
export type FleetChoiceValue = { kind: 'existing'; fleetId: string } | { kind: 'create-new' };

type Props = {
  fleets: Fleet[];
  // When set, the fleet is locked in and we show its details instead of the choice controls.
  selectedFleet: Fleet | null;
  onSelect: (choice: FleetChoiceValue) => void;
  onChange: () => void;
  disabled: boolean;
};

const Container = styled.div(
  ({ theme }) => css`
    max-width: 700px;
    margin: 0 auto ${theme.spacings.md};
    text-align: center;
  `,
);

const Heading = styled.h3(
  ({ theme }) => css`
    margin: 0 0 ${theme.spacings.sm};
    font-size: ${theme.fonts.size.h4};
  `,
);

// Button and dropdown sit side by side, separated by an "or".
const Row = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    justify-content: center;
    gap: ${theme.spacings.md};
  `,
);

const Separator = styled.span(
  ({ theme }) => css`
    color: ${theme.colors.gray[60]};
    font-style: italic;
  `,
);

const SelectField = styled.div`
  flex: 1;
  max-width: 320px;
  text-align: left;
`;

const HiddenLabel = styled.label`
  position: absolute;
  width: 1px;
  height: 1px;
  overflow: hidden;
  clip-path: inset(50%);
`;

// The locked-in fleet: details on the left, a "Change fleet" escape hatch on the right.
const SelectedBox = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: ${theme.spacings.md};
    padding: ${theme.spacings.sm} ${theme.spacings.md};
    border: 1px solid ${theme.colors.cards.border};
    border-radius: ${theme.spacings.xs};
    text-align: left;
  `,
);

const FleetName = styled.div(
  ({ theme }) => css`
    font-weight: 500;
    font-size: ${theme.fonts.size.large};
  `,
);

const FleetDescription = styled.div(
  ({ theme }) => css`
    color: ${theme.colors.gray[60]};
    font-size: ${theme.fonts.size.small};
  `,
);

const FLEET_SELECT_ID = 'onboarding-fleet-select';

const FleetChoice = ({ fleets, selectedFleet, onSelect, onChange, disabled }: Props) => {
  if (selectedFleet) {
    return (
      <Container>
        <Heading>Fleet for this collector</Heading>
        <SelectedBox>
          <div>
            <FleetName>{selectedFleet.name}</FleetName>
            {selectedFleet.description && <FleetDescription>{selectedFleet.description}</FleetDescription>}
          </div>
          <Button onClick={onChange} disabled={disabled}>
            Change fleet
          </Button>
        </SelectedBox>
      </Container>
    );
  }

  const options = fleets.map((fleet) => ({ label: fleet.name, value: fleet.id }));

  return (
    <Container>
      <Heading>Choose a fleet for this collector</Heading>
      <Row>
        <Button bsStyle="primary" onClick={() => onSelect({ kind: 'create-new' })} disabled={disabled}>
          Create new fleet
        </Button>
        <Separator>or</Separator>
        <SelectField>
          <HiddenLabel htmlFor={FLEET_SELECT_ID}>Select existing fleet</HiddenLabel>
          <Select
            inputId={FLEET_SELECT_ID}
            aria-label="Select existing fleet"
            options={options}
            value={null}
            onChange={(value: string) => onSelect({ kind: 'existing', fleetId: value })}
            clearable={false}
            disabled={disabled}
            placeholder="Select existing fleet..."
          />
        </SelectField>
      </Row>
    </Container>
  );
};

export default FleetChoice;
