import React from "react";
import PageHeader from "components/common/PageHeader";

import EmbeddedDBConnectorApp from "./EmbeddedDBConnectorApp";
import { ExternalLink } from "components/common";

const DBConnectorApp = () => {
  return (
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
};


export default DBConnectorApp;