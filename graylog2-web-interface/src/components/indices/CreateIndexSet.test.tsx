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
import { useState } from 'react';
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import asMock from 'helpers/mocking/AsMock';
import usePermissions from 'hooks/usePermissions';
import useIndicesConfiguration from 'hooks/useIndicesConfiguration';
import useIndexSetTemplateDefaults from 'components/indices/IndexSetTemplates/hooks/useIndexSetTemplateDefaults';
import useSelectedIndexSetTemplate from 'components/indices/IndexSetTemplates/hooks/useSelectedTemplate';
import useBuiltInTemplates from 'components/indices/IndexSetTemplates/hooks/useBuiltInTemplates';
import useTemplates from 'components/indices/IndexSetTemplates/hooks/useTemplates';
import useProfileOptions from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfileOptions';
import { DATA_TIERING_TYPE } from 'components/indices/data-tiering';

import CreateIndexSet from './CreateIndexSet';

jest.mock('hooks/usePermissions');
jest.mock('hooks/useIndicesConfiguration');
jest.mock('components/indices/IndexSetTemplates/hooks/useIndexSetTemplateDefaults');
jest.mock('components/indices/IndexSetTemplates/hooks/useSelectedTemplate');
jest.mock('components/indices/IndexSetTemplates/hooks/useBuiltInTemplates');
jest.mock('components/indices/IndexSetTemplates/hooks/useTemplates');
jest.mock('components/indices/IndexSetFieldTypeProfiles/hooks/useProfileOptions');

const rotationStrategies = [
  {
    type: 'org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy',
    default_config: {
      type: 'org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig',
      max_docs_per_index: 20000000,
    },
    json_schema: {
      type: 'object',
      id: 'urn:jsonschema:org:graylog2:indexer:rotation:strategies:MessageCountRotationStrategyConfig',
      properties: { max_docs_per_index: { type: 'integer' }, type: { type: 'string' } },
    },
  },
];

const retentionStrategies = [
  {
    type: 'org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy',
    default_config: {
      type: 'org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig',
      max_number_of_indices: 20,
    },
    json_schema: {
      type: 'object',
      id: 'urn:jsonschema:org:graylog2:indexer:retention:strategies:DeletionRetentionStrategyConfig',
      properties: { max_number_of_indices: { type: 'integer' }, type: { type: 'string' } },
    },
  },
];

const indexSetTemplateDefaults = {
  index_prefix: 'default_index_prefix',
  index_analyzer: 'standard',
  shards: 1,
  replicas: 1,
  index_optimization_max_num_segments: 1,
  index_optimization_disabled: true,
  field_type_refresh_interval: 30,
  field_type_refresh_interval_unit: 'minutes' as const,
  use_legacy_rotation: false,
  rotation_strategy_class: rotationStrategies[0].type,
  rotation_strategy_config: rotationStrategies[0].default_config,
  retention_strategy_class: retentionStrategies[0].type,
  retention_strategy_config: retentionStrategies[0].default_config,
  data_tiering: {
    type: DATA_TIERING_TYPE.HOT_ONLY,
    index_lifetime_min: '10',
    index_lifetime_max: '30',
  },
  field_restrictions: {},
};

const CreateIndexSetWithTemplateModal = () => {
  const [showSelectTemplateModal, setShowSelectTemplateModal] = useState(true);

  return (
    <CreateIndexSet
      showSelectTemplateModal={showSelectTemplateModal}
      setShowSelectTemplateModal={setShowSelectTemplateModal}
    />
  );
};

describe('CreateIndexSet', () => {
  beforeEach(() => {
    asMock(usePermissions).mockReturnValue({ isPermitted: () => true, isAnyPermitted: () => true, isAdmin: false });
    asMock(useIndicesConfiguration).mockReturnValue({
      rotationStrategies,
      retentionStrategies,
      retentionStrategiesContext: { max_index_retention_period: 'P1D' },
      rotationStrategiesContext: undefined,
      isLoading: false,
    });
    asMock(useIndexSetTemplateDefaults).mockReturnValue({
      loadingIndexSetTemplateDefaults: false,
      indexSetTemplateDefaults,
    });
    asMock(useSelectedIndexSetTemplate).mockReturnValue({
      selectedIndexSetTemplate: undefined,
      setSelectedIndexSetTemplate: jest.fn(),
    });
    asMock(useBuiltInTemplates).mockReturnValue({ data: [], isLoading: false, refetch: jest.fn() });
    asMock(useTemplates).mockReturnValue({
      data: { list: [], pagination: { total: 0 }, attributes: [] },
      isLoading: false,
      refetch: jest.fn(),
    });
    asMock(useProfileOptions).mockReturnValue({ isLoading: false, options: [], refetch: () => {} });
  });

  it('focuses the title field after closing the template modal', async () => {
    render(<CreateIndexSetWithTemplateModal />);

    await screen.findByRole('dialog', { name: /index set templates/i });

    await userEvent.keyboard('{Escape}');

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());

    expect(await screen.findByRole('textbox', { name: /title/i })).toHaveFocus();
  });
});
