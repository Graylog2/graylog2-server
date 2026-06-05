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
import { useState, useCallback, useRef } from 'react';
import styled, { css } from 'styled-components';

import { Spinner } from 'components/common';
import { getMajorAndMinorVersion } from 'util/Version';

import PlatformPicker from './onboarding/PlatformPicker';
import InstallCommand from './onboarding/InstallCommand';
import WaitingForConnection from './onboarding/WaitingForConnection';
import ConnectionSuccess from './onboarding/ConnectionSuccess';
import FleetSelector from './onboarding/FleetSelector';
import PLATFORMS from './onboarding/platforms';
import type { PlatformId } from './onboarding/platforms';
import DEFAULT_SOURCES from './onboarding/defaultSources';

import { useCollectorsConfig, useCollectorsMutations, useFleets } from '../hooks';

type Phase = 'pick' | 'waiting' | 'connected';

// The platform picker (700px) and fleet selector (400px) own their own centered widths.
// The install/connected body gets a wider column so the command, stat cards, and asset grid have room.
const BodyContainer = styled.div(
  ({ theme }) => css`
    max-width: 960px;
    margin: 0 auto ${theme.spacings.lg};
  `,
);

const formatDate = () => {
  const now = new Date();

  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`;
};

const FirstOnboarding = () => {
  const [phase, setPhase] = useState<Phase>('pick');
  const [selectedPlatform, setSelectedPlatform] = useState<PlatformId | null>(null);
  const [installCommand, setInstallCommand] = useState<string>('');
  const [selectedFleetId, setSelectedFleetId] = useState<string | null>(null);
  const [createNewFleet, setCreateNewFleet] = useState(false);

  const tokenRef = useRef<string | null>(null);
  const fleetIdRef = useRef<string | null>(null);

  const { data: config, isLoading: isConfigLoading } = useCollectorsConfig();
  const { data: fleets, isLoading: isFleetsLoading } = useFleets();
  const {
    createFleet, isCreatingFleet,
    createSource,
    createEnrollmentToken, isCreatingEnrollmentToken,
  } = useCollectorsMutations();

  const buildCommand = useCallback(
    (platformId: PlatformId, token: string) => {
      if (!config) return '';

      const platform = PLATFORMS.find((p) => p.id === platformId);

      if (!platform) return '';

      return platform.commandTemplate(config.http.hostname, config.http.port, token);
    },
    [config],
  );

  const createOnboardingFleet = useCallback(async () => {
    const version = getMajorAndMinorVersion();
    const fleet = await createFleet({
      name: `Onboarding - ${formatDate()}`,
      description: `Created by Graylog ${version} onboarding wizard`,
    });

    await Promise.all(
      DEFAULT_SOURCES.map((source) => createSource({ fleetId: fleet.id, source, silent: true })),
    );

    return fleet.id;
  }, [createFleet, createSource]);

  const resolveFleetId = useCallback(async (): Promise<string | null> => {
    if (fleetIdRef.current) return fleetIdRef.current;

    if (!fleets) return null;

    if (fleets.length === 0 || createNewFleet) {
      const newFleetId = await createOnboardingFleet();
      fleetIdRef.current = newFleetId;

      return newFleetId;
    }

    if (fleets.length === 1) {
      fleetIdRef.current = fleets[0].id;

      return fleets[0].id;
    }

    if (selectedFleetId) {
      fleetIdRef.current = selectedFleetId;

      return selectedFleetId;
    }

    return null;
  }, [fleets, createNewFleet, selectedFleetId, createOnboardingFleet]);

  const handleFleetSelect = useCallback((fleetId: string | null, isCreateNew: boolean) => {
    setSelectedFleetId(fleetId);
    setCreateNewFleet(isCreateNew);
    fleetIdRef.current = null;
    tokenRef.current = null;
  }, []);

  const handlePlatformSelect = useCallback(
    async (platformId: PlatformId) => {
      if (!config) return;

      setSelectedPlatform(platformId);

      if (tokenRef.current) {
        setInstallCommand(buildCommand(platformId, tokenRef.current));
        setPhase('waiting');

        return;
      }

      try {
        const fleetId = await resolveFleetId();

        if (!fleetId) return;

        const response = await createEnrollmentToken({
          name: 'onboarding',
          fleetId,
          expiresIn: 'P1D',
        });

        tokenRef.current = response.token;
        setInstallCommand(buildCommand(platformId, response.token));
        setPhase('waiting');
      } catch {
        // Error notification handled by useCollectorsMutations onError callback
      }
    },
    [config, resolveFleetId, createEnrollmentToken, buildCommand],
  );

  const handleSimulateConnection = useCallback(() => {
    setPhase('connected');
  }, []);

  if (isConfigLoading || isFleetsLoading) return <Spinner />;

  const isBusy = isCreatingFleet || isCreatingEnrollmentToken;
  const showFleetSelector = (fleets?.length ?? 0) > 1;
  const needsFleetSelection = showFleetSelector && !selectedFleetId && !createNewFleet;

  return (
    <div>
      {showFleetSelector && (
        <FleetSelector
          fleets={fleets!}
          selectedFleetId={selectedFleetId}
          onSelect={handleFleetSelect}
          disabled={isBusy}
        />
      )}

      <PlatformPicker
        onSelect={handlePlatformSelect}
        selectedPlatform={selectedPlatform}
        disabled={isBusy || needsFleetSelection}
      />

      {phase !== 'pick' && selectedPlatform && (
        <BodyContainer>
          {phase === 'waiting' && (
            <>
              <InstallCommand
                command={installCommand}
                platformLabel={PLATFORMS.find((p) => p.id === selectedPlatform)?.label ?? ''}
                tokenDuration='P1D'
              />
              <WaitingForConnection onSimulateConnection={handleSimulateConnection} />
            </>
          )}

          {phase === 'connected' && <ConnectionSuccess platformId={selectedPlatform} />}
        </BodyContainer>
      )}
    </div>
  );
};

export default FirstOnboarding;
