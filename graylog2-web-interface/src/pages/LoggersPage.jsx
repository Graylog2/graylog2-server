import React from 'react';

import { DocumentTitle, PageHeader } from 'components/common';
import { LoggerOverview } from 'components/loggers';

const LoggersPage = React.createClass({
  render() {
    return (
      <DocumentTitle title="Logging">
        <span>
          <PageHeader title="Logging">
            <span>
              This section controls logging of the Graylog architecture and allows you to change log
              levels on the fly. Note that log levels are reset to their defaults after you restart
              the affected service.
            </span>
          </PageHeader>
          <LoggerOverview />
        </span>
      </DocumentTitle>
    );
  },
});

export default LoggersPage;
