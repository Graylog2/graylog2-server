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
