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
import { useQueryClient } from '@tanstack/react-query';

import { Spinner } from 'components/common';
import { getMajorAndMinorVersion } from 'util/Version';
import useHistory from 'routing/useHistory';
import Routes from 'routing/Routes';

import PlatformPicker from './onboarding/PlatformPicker';
import InstallCommand from './onboarding/InstallCommand';
import WaitingForConnection from './onboarding/WaitingForConnection';
import FleetChoice from './onboarding/FleetChoice';
import type { FleetChoiceValue } from './onboarding/FleetChoice';
import PLATFORMS from './onboarding/platforms';
import type { PlatformId } from './onboarding/platforms';
import DEFAULT_SOURCES from './onboarding/defaultSources';

import { useCollectorsConfig, useCollectorsMutations, useFleets } from '../hooks';
// Imported from the concrete module (not the hooks index) so tests that automock the index
// still see the real cache key.
import { INSTANCES_KEY_PREFIX } from '../hooks/useInstanceQueries';
import type { Fleet, CollectorInstanceView } from '../types';

// 'setup' = still collecting platform/fleet; 'waiting' = command box is live.
type Phase = 'setup' | 'waiting';

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
  const [phase, setPhase] = useState<Phase>('setup');
  const [selectedPlatform, setSelectedPlatform] = useState<PlatformId | null>(null);
  const [installCommand, setInstallCommand] = useState<string>('');
  // Only ever set by the fleet-choice UI, which renders only when more than one fleet exists.
  const [fleetChoice, setFleetChoice] = useState<FleetChoiceValue | null>(null);
  // The fleet the collector will enroll into, once resolved (looked up or freshly created).
  const [resolvedFleet, setResolvedFleet] = useState<Fleet | null>(null);

  // The enrollment token is minted once and reused while switching platforms.
  const tokenRef = useRef<string | null>(null);

  const queryClient = useQueryClient();
  const history = useHistory();

  const { data: config, isLoading: isConfigLoading } = useCollectorsConfig();
  const { data: fleets, isLoading: isFleetsLoading } = useFleets();
  const { createFleet, isCreatingFleet, createSource, createEnrollmentToken, isCreatingEnrollmentToken } =
    useCollectorsMutations();

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

    await Promise.all(DEFAULT_SOURCES.map((source) => createSource({ fleetId: fleet.id, source, silent: true })));

    return fleet;
  }, [createFleet, createSource]);

  // The fleet to use without asking the user: none -> create one, exactly one -> use it,
  // more than one -> null (the user must decide via the fleet-choice UI).
  const autoChoice = useCallback((): FleetChoiceValue | null => {
    if (!fleets || fleets.length === 0) return { kind: 'create-new' };
    if (fleets.length === 1) return { kind: 'existing', fleetId: fleets[0].id };

    return null;
  }, [fleets]);

  // Both gates (platform, fleet) converge here once both are known. Builds the command box.
  const showCommand = useCallback(
    async (platformId: PlatformId, choice: FleetChoiceValue) => {
      try {
        if (!tokenRef.current) {
          const fleet =
            choice.kind === 'create-new' ? await createOnboardingFleet() : fleets?.find((f) => f.id === choice.fleetId);

          if (!fleet) return;

          // Reflect the fleet right away so the box lands on its details view without a flash of the prompt.
          setResolvedFleet(fleet);

          const { token } = await createEnrollmentToken({ name: 'onboarding', fleetId: fleet.id, expiresIn: 'P1D' });
          tokenRef.current = token;
        }

        setInstallCommand(buildCommand(platformId, tokenRef.current));
        setPhase('waiting');
      } catch {
        // Error notification handled by useCollectorsMutations onError callback
      }
    },
    [fleets, createOnboardingFleet, createEnrollmentToken, buildCommand],
  );

  const handlePlatformSelect = useCallback(
    (platformId: PlatformId) => {
      setSelectedPlatform(platformId);

      // 0/1-fleet falls straight through; >1 fleets waits for the user's choice.
      const choice = fleetChoice ?? autoChoice();
      if (choice) showCommand(platformId, choice);
    },
    [fleetChoice, autoChoice, showCommand],
  );

  const handleFleetChoice = useCallback(
    (choice: FleetChoiceValue) => {
      setFleetChoice(choice);
      tokenRef.current = null; // fleet changed -> a new token is needed

      if (selectedPlatform) showCommand(selectedPlatform, choice);
    },
    [selectedPlatform, showCommand],
  );

  // "Change fleet": drop the resolved fleet and token, and fall back to the choice UI.
  const handleChangeFleet = useCallback(() => {
    setFleetChoice(null);
    setResolvedFleet(null);
    tokenRef.current = null;
    setPhase('setup');
  }, []);

  const handleConnected = useCallback(
    (instance: CollectorInstanceView) => {
      // Seed the destination page's lookup so it renders without a refetch round trip.
      queryClient.setQueryData([...INSTANCES_KEY_PREFIX, 'single', instance.instance_uid], instance);
      // The overview's cached stats still say zero instances; refresh so Back shows the real overview.
      queryClient.invalidateQueries({ queryKey: ['collectors'] });
      history.pushWithState(Routes.SYSTEM.COLLECTORS.ONBOARDING_INSTANCE(instance.instance_uid), {
        platformId: selectedPlatform,
        fleetName: resolvedFleet?.name,
      });
    },
    [queryClient, history, selectedPlatform, resolvedFleet?.name],
  );

  if (isConfigLoading || isFleetsLoading) return <Spinner />;

  const isBusy = isCreatingFleet || isCreatingEnrollmentToken;
  // Show the fleet box whenever an existing fleet could be chosen. With exactly one fleet it
  // auto-resolves (no prompt — see autoChoice), but stays visible so the user can change it.
  const showFleetChoice = (fleets?.length ?? 0) >= 1;

  return (
    <div>
      {/* 1. Always: pick the operating system. */}
      <PlatformPicker onSelect={handlePlatformSelect} selectedPlatform={selectedPlatform} disabled={isBusy} />

      {/* 2. Only when a platform is picked and at least one fleet exists.
            Shows the choice controls until a fleet is resolved, then its details. */}
      {selectedPlatform && showFleetChoice && (
        <FleetChoice
          fleets={fleets!}
          selectedFleet={resolvedFleet}
          onSelect={handleFleetChoice}
          onChange={handleChangeFleet}
          disabled={isBusy}
        />
      )}

      {/* 3. The command box, once the preconditions are satisfied. */}
      {phase === 'waiting' && selectedPlatform && (
        <BodyContainer>
          <InstallCommand
            command={installCommand}
            platformLabel={PLATFORMS.find((p) => p.id === selectedPlatform)?.label ?? ''}
            tokenDuration="P1D"
          />
          <WaitingForConnection key={resolvedFleet?.id} fleetId={resolvedFleet?.id} onConnected={handleConnected} />
        </BodyContainer>
      )}
    </div>
  );
};

export default FirstOnboarding;
