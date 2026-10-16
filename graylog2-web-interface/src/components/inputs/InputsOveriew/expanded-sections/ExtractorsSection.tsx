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
import { useTranslation } from 'react-i18next';
import { useQuery } from '@tanstack/react-query';

import { Extractors } from '@graylog/server-api';

import type { InputSummary } from 'hooks/usePaginatedInputs';
import { Spinner } from 'components/common';
import { Table } from 'components/bootstrap';
import { defaultOnError } from 'util/conditional/onError';
import i18n from 'i18n';

type ExtractorEntry = {
  id: string;
  title: string;
  type: string;
  source_field: string;
  target_field: string;
};

type ExtractorSummaryList = {
  total: number;
  extractors: Array<ExtractorEntry>;
};

const fetchExtractors = (inputId: string): Promise<ExtractorSummaryList> => Extractors.list(inputId);

const useInputExtractors = (inputId: string) =>
  useQuery({
    queryKey: ['input', inputId, 'extractors'],
    queryFn: () =>
      defaultOnError(
        fetchExtractors(inputId),
        i18n.t('overview.sections.load_extractors_failed', { ns: 'inputs' }),
        i18n.t('overview.sections.could_not_load_extractors_title', { ns: 'inputs' }),
      ),
  });

type Props = {
  input: InputSummary;
};

const ExtractorsSection = ({ input }: Props) => {
  const { t } = useTranslation('inputs');
  const { data, isInitialLoading, isError } = useInputExtractors(input.id);

  if (isInitialLoading) {
    return <Spinner />;
  }

  if (isError) {
    return <p>{t('overview.sections.could_not_load_extractors')}</p>;
  }

  const extractors = data?.extractors ?? [];

  if (extractors.length === 0) {
    return <p>{t('overview.sections.no_extractors')}</p>;
  }

  return (
    <Table bordered condensed>
      <thead>
        <tr>
          <th>{t('overview.sections.extractor_title')}</th>
          <th>{t('overview.sections.extractor_type')}</th>
          <th>{t('overview.sections.source_field')}</th>
          <th>{t('overview.sections.target_field')}</th>
        </tr>
      </thead>
      <tbody>
        {extractors.map((extractor) => (
          <tr key={extractor.id}>
            <td>{extractor.title}</td>
            <td>{extractor.type}</td>
            <td>{extractor.source_field}</td>
            <td>{extractor.target_field}</td>
          </tr>
        ))}
      </tbody>
    </Table>
  );
};

export default ExtractorsSection;
