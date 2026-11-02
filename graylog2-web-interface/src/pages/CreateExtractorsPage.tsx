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
import { useMemo } from 'react';
import { Trans, useTranslation } from 'react-i18next';

import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import EditExtractor from 'components/extractors/EditExtractor';
import DocsHelper from 'util/DocsHelper';
import StringUtils from 'util/StringUtils';
import Routes from 'routing/Routes';
import { newExtractor } from 'hooks/useExtractors';
import useMessage from 'views/hooks/useMessage';
import useParams from 'routing/useParams';
import useHistory from 'routing/useHistory';
import useQuery from 'routing/useQuery';
import useInput from 'hooks/useInput';

type QueryParameters = {
  example_index: string;
  example_id: string;
  extractor_type: string;
  field: string;
};

const CreateExtractorsPage = () => {
  const { t } = useTranslation('extractors');
  const params = useParams();
  const history = useHistory();
  const {
    example_index: exampleIndex,
    example_id: exampleId,
    extractor_type: extractorType,
    field,
  } = useQuery() as QueryParameters;
  const { data: exampleMessage, isInitialLoading: messageIsLoading } = useMessage(exampleIndex, exampleId);
  const { data: input, isInitialLoading: inputIsLoading } = useInput(params.inputId);
  const isLoading = messageIsLoading || inputIsLoading;
  const extractor = useMemo(() => newExtractor(extractorType, field), [extractorType, field]);

  const _extractorSaved = () => {
    const url = input.global
      ? Routes.global_input_extractors(params.inputId)
      : Routes.local_input_extractors(params.nodeId, params.inputId);

    history.push(url);
  };

  if (isLoading) {
    return <Spinner />;
  }

  const stringifiedExampleMessage = StringUtils.stringify(exampleMessage.fields[field]);

  return (
    <DocumentTitle title={t('pages.new_title', { title: input.title })}>
      <div>
        <PageHeader
          title={
            <span>
              <Trans t={t} i18nKey="pages.new_header" values={{ title: input.title }} components={{ em: <em /> }} />
            </span>
          }
          documentationLink={{
            title: t('documentation_title'),
            path: DocsHelper.PAGES.EXTRACTORS,
          }}>
          <span>{t('pages.description')}</span>
        </PageHeader>
        <EditExtractor
          action="create"
          extractor={extractor}
          inputId={input.id}
          exampleMessage={stringifiedExampleMessage}
          onSave={_extractorSaved}
        />
      </div>
    </DocumentTitle>
  );
};

export default CreateExtractorsPage;
