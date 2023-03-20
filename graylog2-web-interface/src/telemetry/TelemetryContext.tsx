import * as React from 'react';
// import type { PostHog } from 'posthog-js';

import { singleton } from 'logic/singleton';

type ContextType = {
  sendTelemetry: (eventType: string, event: Object) => void,
}
const TelemetryContext = React.createContext<ContextType>({
  sendTelemetry: () => {},
});

export default singleton('contexts.TelemetryContext', () => TelemetryContext);
