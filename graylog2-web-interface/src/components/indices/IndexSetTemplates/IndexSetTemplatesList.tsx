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
import React, { useCallback } from 'react';

import { PaginatedEntityTable } from 'components/common';
import type { Sort } from 'stores/PaginationTypes';
import type { IndexSetTemplate }
  from 'components/indices/IndexSetTemplates/types';
import { fetchIndexSetTemplates, keyFn }
  from 'components/indices/IndexSetTemplates/hooks/useTemplates';
import TemplateActions from 'components/indices/IndexSetTemplates/TemplateActions';
import customColumnRenderers from 'components/indices/IndexSetTemplates/helpers/customColumnRenderers';

export const DEFAULT_LAYOUT = {
  entityTableId: 'index-set-template',
  defaultPageSize: 20,
  defaultSort: { attributeId: 'title', direction: 'asc' } as Sort,
  defaultDisplayedAttributes: ['title', 'built_in', 'description'],
};

const COLUMNS_ORDER = ['title', 'built_in', 'description'];

const IndexSetTemplatesList = () => {
  const templateActions = useCallback(({ id, title, built_in, default: isDefault, enabled: isEnabled }: IndexSetTemplate) => (
    <TemplateActions id={id} title={title} built_in={built_in} isDefault={isDefault} isEnabled={isEnabled} />
  ), []);

  return (
    <PaginatedEntityTable<IndexSetTemplate> humanName="index set templates"
                                            columnsOrder={COLUMNS_ORDER}
                                            entityActions={templateActions}
                                            tableLayout={DEFAULT_LAYOUT}
                                            fetchEntities={fetchIndexSetTemplates}
                                            keyFn={keyFn}
                                            entityAttributesAreCamelCase={false}
                                            columnRenderers={customColumnRenderers}
                                            searchPlaceholder="Search for index set template" />
  );
};

export default IndexSetTemplatesList;
