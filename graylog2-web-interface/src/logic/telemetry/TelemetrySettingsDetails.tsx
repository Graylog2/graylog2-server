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
import React, { useEffect, useState } from 'react';

import SectionComponent from 'components/common/Section/SectionComponent';
import { Spinner, ReadOnlyFormGroup } from 'components/common';
import type { UserTelemetrySettings } from 'stores/telemetry/TelemetrySettingsStore';
import {
  TelemetrySettingsActions,
} from 'stores/telemetry/TelemetrySettingsStore';
import TelemetryInfoText from 'logic/telemetry/TelemetryInfoText';

const TelemetrySettingsDetails = () => {
  const [settings, setSettings] = useState<UserTelemetrySettings | undefined>(undefined);

  useEffect(() => {
    TelemetrySettingsActions.get().then((result) => {
      setSettings(result);
    });
  }, []);

  if (!settings) {
    return <Spinner />;
  }

  return (
    <SectionComponent title="Telemetry">
      <TelemetryInfoText />
      <ReadOnlyFormGroup label="Telemetry" value={settings.telemetry_enabled ?? false} />
    </SectionComponent>
  );
};

export default TelemetrySettingsDetails;
