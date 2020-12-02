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
import React from 'react';

import { DocumentTitle, PageHeader } from 'components/common';
import { LoggerOverview } from 'components/loggers';

class LoggersPage extends React.Component {
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
  }
}

export default LoggersPage;
