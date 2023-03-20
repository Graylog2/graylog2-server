import type { PostHog } from 'posthog-js';

import history from 'util/History';

function navigationTelemetry(posthog: PostHog) {
  // listen to page changes
  history.listen(() => {
    posthog?.capture('$pageview');
  });
}

export default navigationTelemetry;
