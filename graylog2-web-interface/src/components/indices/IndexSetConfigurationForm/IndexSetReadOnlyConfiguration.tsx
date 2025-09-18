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

import { FormikInput } from 'components/common';

const _validateIndexPrefix = (value: string) => {
  let error: string;

  if (value?.length === 0) {
    error = 'Invalid index prefix: cannot be empty';
  } else if (value?.indexOf('_') === 0 || value?.indexOf('-') === 0 || value?.indexOf('+') === 0) {
    error = 'Invalid index prefix: must start with a letter or number';
  } else if (value?.toLocaleLowerCase() !== value) {
    error = 'Invalid index prefix: must be lower case';
  } else if (!value?.match(/^[a-z0-9][a-z0-9_\-+]*$/)) {
    error = "Invalid index prefix: must only contain letters, numbers, '_', '-' and '+'";
  }

  return error;
};

const IndexSetReadOnlyConfiguration = ({
  hiddenFields,
  immutableFields,
  ignoreFieldRestrictions,
}: {
  hiddenFields: string[];
  immutableFields: string[];
  ignoreFieldRestrictions: boolean;
}) => {
  const indexPrefixHelp = (
    <span>
      A <strong>unique</strong> prefix used in Elasticsearch indices belonging to this index set. The prefix must start
      with a letter or number, and can only contain letters, numbers, &apos;_&apos;, &apos;-&apos; and &apos;+&apos;.
    </span>
  );

  return (
    <>
      <FormikInput
        type="text"
        id="index-prefix"
        label="Index prefix"
        name="index_prefix"
        help={indexPrefixHelp}
        validate={_validateIndexPrefix}
        required
      />
      {(!hiddenFields.includes('index_analyzer') || ignoreFieldRestrictions) && (
        <FormikInput
          type="text"
          id="index-analyzer"
          label="Analyzer"
          name="index_analyzer"
          help="Elasticsearch analyzer for this index set."
          required
          disabled={immutableFields.includes('index_analyzer') && !ignoreFieldRestrictions}
        />
      )}
    </>
  );
};

export default IndexSetReadOnlyConfiguration;
