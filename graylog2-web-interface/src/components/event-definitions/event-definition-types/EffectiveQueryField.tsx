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
import { useState, useEffect } from 'react';
import styled from 'styled-components';
import omit from 'lodash/omit';

import fetch from 'logic/rest/FetchProvider';
import { qualifyUrl } from 'util/URLUtils';
import { ClipboardButton, Icon, Spinner } from 'components/common';
import type { SearchFilter } from 'components/event-definitions/event-definitions-types';

const EFFECTIVE_QUERY_URL = '/plugins/org.graylog.plugins.searchfilters/search_filters/effective_query';
// Debounce so typing in the base query doesn't fire a request per keystroke.
const REQUEST_DEBOUNCE_MS = 250;

// Mirror the Search Query InputRow: a field that flex-grows plus a small control to its right,
// so this field lines up with the (validation-icon-narrowed) query input above.
const Row = styled.div`
  display: flex;
  align-items: flex-start;
  gap: 5px;
`;

// Read-only field styled to mirror the Search Query input above it.
const Field = styled.code(
  ({ theme }) => `
  flex: 1;
  padding: 6px 8px;
  background-color: ${theme.colors.global.contentBackground};
  border: 1px solid ${theme.colors.input.border};
  border-radius: 4px;
  color: ${theme.colors.gray[30]};
  white-space: pre-wrap;
  word-break: break-word;
`,
);

type Props = {
  queryString: string;
  filters: SearchFilter[];
};

const EffectiveQueryField = ({ queryString, filters }: Props) => {
  const [effectiveQuery, setEffectiveQuery] = useState('');
  const [loading, setLoading] = useState(true);

  // Serialize the request into a stable primitive so the effect only re-runs when it actually changes.
  // The body is parsed back inside the effect to avoid referencing per-render values (refs/objects)
  // from the effect, which the React compiler lint disallows.
  const payloadKey = JSON.stringify({
    query_string: queryString ?? '',
    filters: (filters ?? []).map((filter) => omit(filter, 'frontendId')),
  });

  useEffect(() => {
    const timer = setTimeout(() => {
      fetch('POST', qualifyUrl(EFFECTIVE_QUERY_URL), JSON.parse(payloadKey))
        .then((response) => setEffectiveQuery(response.effective_query))
        .catch(() => setEffectiveQuery('(failed to render effective query)'))
        .finally(() => setLoading(false));
    }, REQUEST_DEBOUNCE_MS);

    return () => clearTimeout(timer);
  }, [payloadKey]);

  return (
    <Row>
      <Field>{loading && !effectiveQuery ? <Spinner text="" delay={0} /> : effectiveQuery}</Field>
      <ClipboardButton
        title={<Icon name="content_copy" />}
        bsSize="xsmall"
        text={effectiveQuery}
        buttonTitle="Copy effective query"
      />
    </Row>
  );
};

export default EffectiveQueryField;
