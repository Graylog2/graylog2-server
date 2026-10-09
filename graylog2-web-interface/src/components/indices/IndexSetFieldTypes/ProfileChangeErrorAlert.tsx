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
import * as React from 'react';

import { Alert } from 'components/bootstrap';
import type { IndexSetProfileChangeResult } from 'components/indices/IndexSetFieldTypes/types';

type Props = {
  results: Array<IndexSetProfileChangeResult>;
  titles: Record<string, string>;
};

const ProfileChangeErrorAlert = ({ results, titles }: Props) => (
  <Alert bsStyle="danger" title="Changing the field type profile failed for some index sets">
    <ul>
      {results.map(({ indexSetId, failures, errors }) => (
        <li key={indexSetId}>
          <b>{titles[indexSetId] ?? indexSetId}</b>
          <ul>
            {[...failures, ...errors].map((message) => (
              <li key={message}>
                <i>{message}</i>
              </li>
            ))}
          </ul>
        </li>
      ))}
    </ul>
  </Alert>
);

export default ProfileChangeErrorAlert;
