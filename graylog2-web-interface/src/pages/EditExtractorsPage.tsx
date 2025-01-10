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
import { useEffect, useState } from 'react';

import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import EditExtractor from 'components/extractors/EditExtractor';
import DocsHelper from 'util/DocsHelper';
import Routes from 'routing/Routes';
import { ExtractorsActions, ExtractorsStore } from 'stores/extractors/ExtractorsStore';
import { InputsActions, InputsStore } from 'stores/inputs/InputsStore';
import { UniversalSearchStore } from 'stores/search/UniversalSearchStore';
import { useStore } from 'stores/connect';
import useParams from 'routing/useParams';
import useHistory from 'routing/useHistory';

const EditExtractorsPage = () => {
  const history = useHistory();
  const { extractor } = useStore(ExtractorsStore);
  const { input } = useStore(InputsStore);
  const { inputId, extractorId, nodeId } = useParams<{ inputId: string, extractorId: string, nodeId: string }>();
  const [exampleMessage, setExampleMessage] = useState<{ fields?: { [key: string]: any }}>({});

  useEffect(() => {
    InputsActions.get(inputId);
    ExtractorsActions.get(inputId, extractorId);

    UniversalSearchStore.search('relative', `gl2_source_input:${inputId} OR gl2_source_radio_input:${inputId}`, { relative: 3600 }, undefined, 1)
      .then((response) => {
        if (response.total_results > 0) {
          setExampleMessage(response.messages[0]);
        } else {
          setExampleMessage({});
        }
      });
  }, [extractorId, inputId]);

  const _isLoading = !(input && extractor && exampleMessage);

  const _extractorSaved = () => {
    let url;

    if (input.global) {
      url = Routes.global_input_extractors(inputId);
    } else {
      url = Routes.local_input_extractors(nodeId, inputId);
    }

    history.push(url);
  };

  // TODO:
  // - Redirect when extractor or input were deleted

  if (_isLoading) {
    return <Spinner />;
  }

  return (
    <DocumentTitle title={`Edit extractor ${extractor.title}`}>
      <PageHeader title={<span>Edit extractor <em>{extractor.title}</em> for input <em>{input.title}</em></span>}
                  documentationLink={{
                    title: 'Extractors documentation',
                    path: DocsHelper.PAGES.EXTRACTORS,
                  }}>
        <span>
          Extractors are applied on every message that is received by an input. Use them to extract and transform{' '}
          any text data into fields that allow you easy filtering and analysis later on.
        </span>
      </PageHeader>
      <EditExtractor action="edit"
                     extractor={extractor}
                     inputId={input.id}
                     exampleMessage={exampleMessage.fields ? exampleMessage.fields[extractor.source_field] : undefined}
                     onSave={_extractorSaved} />

    </DocumentTitle>
  );
};

export default EditExtractorsPage;
