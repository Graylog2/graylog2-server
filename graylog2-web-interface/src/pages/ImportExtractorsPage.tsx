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
import { useEffect, useState } from 'react';

import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import ImportExtractors from 'components/extractors/ImportExtractors';
import type { ParamsContext } from 'routing/withParams';
import withParams from 'routing/withParams';
import { InputsActions } from 'stores/inputs/InputsStore';
import type { Input } from 'components/messageloaders/Types';

type Props = ParamsContext;

const ImportExtractorsPage = ({ params }: Props) => {
  const [input, setInput] = useState<Input>();

  useEffect(() => {
    InputsActions.get(params.inputId).then((_input) => setInput(_input));
  }, []);

  const _isLoading = !input;

  if (_isLoading) {
    return <Spinner />;
  }

  return (
    <DocumentTitle title={`Import extractors to ${input.title}`}>
      <div>
        <PageHeader title={<span>Import extractors to <em>{input.title}</em></span>}>
          <span>
            Exported extractors can be imported to an input. All you need is the JSON export of extractors from any
            other Graylog setup or from{' '}
            <a href="https://marketplace.graylog.org/" rel="noopener noreferrer" target="_blank">
              the Graylog Marketplace
            </a>.
          </span>
        </PageHeader>
        <ImportExtractors input={input} />
      </div>
    </DocumentTitle>
  );
};

export default withParams(ImportExtractorsPage);
