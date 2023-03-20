import * as React from 'react';
// import type { PostHog } from 'posthog-js';

import { singleton } from 'logic/singleton';

const TelemetryInitContext = React.createContext({});

export default singleton('contexts.TelemetryInitContext', () => TelemetryInitContext);
