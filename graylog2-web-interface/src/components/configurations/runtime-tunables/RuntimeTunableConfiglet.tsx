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

import { Button } from 'components/bootstrap';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';

import TunableSummary from './TunableSummary';
import TunableEditModal from './TunableEditModal';
import useRuntimeTunableMutations from './useRuntimeTunableMutations';
import type { RuntimeTunable, TunableValues } from './types';
import { changedProperties, toFormValues } from './formValues';

type Props = {
  tunable: RuntimeTunable;
  featureId: string;
};

const RuntimeTunableConfiglet = ({ tunable, featureId }: Props) => {
  const [showModal, setShowModal] = useState(false);
  const { updateTunable, resetTunable } = useRuntimeTunableMutations();
  const sendTelemetry = useSendTelemetry(featureId);
  const telemetryDetails = { tunable_id: tunable.id, feature_id: featureId };

  const save = (values: TunableValues) =>
    updateTunable({ id: tunable.id, values }).then(() =>
      sendTelemetry(TELEMETRY_EVENT_TYPE.CONFIGURATIONS.RUNTIME_TUNABLE_UPDATED, {
        app_action_value: 'configuration-save',
        ...telemetryDetails,
        changed_fields: changedProperties(toFormValues(tunable.schema, tunable.value), values),
      }),
    );
  const reset = () =>
    resetTunable(tunable.id).then(() =>
      sendTelemetry(TELEMETRY_EVENT_TYPE.CONFIGURATIONS.RUNTIME_TUNABLE_RESET, {
        app_action_value: 'configuration-reset',
        ...telemetryDetails,
      }),
    );

  return (
    <div>
      <h2>{tunable.title}</h2>
      {tunable.description && <p>{tunable.description}</p>}
      <TunableSummary tunable={tunable} />
      {tunable.canEdit && (
        <Button type="button" bsSize="xs" bsStyle="info" onClick={() => setShowModal(true)}>
          Edit configuration
        </Button>
      )}
      {showModal && (
        <TunableEditModal tunable={tunable} onSave={save} onReset={reset} onClose={() => setShowModal(false)} />
      )}
    </div>
  );
};

export default RuntimeTunableConfiglet;
