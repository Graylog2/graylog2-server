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

import { SearchForm } from 'components/common';
import QueryHelper from 'components/common/QueryHelper';

const queryExamples = (
  <>
    <p>
      Find sidecars that did not communicate with Graylog since a date:<br />
      <kbd>{'last_seen:<=2018-04-10'}</kbd><br />
    </p>
    <p>
      Find sidecars with <code>failing</code> or <code>unknown</code> status:<br />
      <kbd>status:failing status:unknown</kbd><br />
    </p>
  </>
);

const fieldMap = {
  status: 'Status of the sidecar as it appears in the list, i.e. running, failing, or unknown',
  operating_system: 'Operating system the sidecar is running on',
  last_seen: 'Date and time when the sidecar last communicated with Graylog',
  node_id: 'Identifier of the sidecar',
  sidecar_version: 'Sidecar version',
};

const queryHelp = (
  <QueryHelper entityName="sidecar" example={queryExamples} commonFields={['name']} fieldMap={fieldMap} />
);

type Props = React.PropsWithChildren<{
  query: string,
  onSearch: (query: string) => void,
  onReset: () => void,
}>;

const SidecarSearchForm = ({ query, onSearch, onReset, children }: Props) => (
  <SearchForm query={query}
              onSearch={onSearch}
              onReset={onReset}
              placeholder="Find sidecars"
              queryHelpComponent={queryHelp}
              topMargin={0}
              useLoadingState>
    {children}
  </SearchForm>
);

export default SidecarSearchForm;
