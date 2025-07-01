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
import React from "react";

import PageHeader from "components/common/PageHeader";
import { ExternalLink } from "components/common";

import EmbeddedDBConnectorApp from "./EmbeddedDBConnectorApp";

const DBConnectorApp = () => (
    <>
      <PageHeader title="Database Connector">
        <span>This feature retrieves logs from your databases - MySQL, Oracle, SQL, DB2, PostgreSQL and MongoDB.</span>
        <p>
          You need to have{" "}
          <ExternalLink href="">
            Database Connection
          </ExternalLink>
          .{" "}
        </p>
      </PageHeader>
      <EmbeddedDBConnectorApp />
    </>
  );


export default DBConnectorApp;