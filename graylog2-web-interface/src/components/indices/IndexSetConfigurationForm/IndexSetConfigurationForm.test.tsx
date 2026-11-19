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
import { render, screen, within } from 'wrappedTestingLibrary';
import { waitFor } from 'wrappedTestingLibrary/hooks';
import userEvent from '@testing-library/user-event';

import useIndexSetTemplateDefaults from 'components/indices/IndexSetTemplates/hooks/useIndexSetTemplateDefaults';
import useSelectedIndexSetTemplate from 'components/indices/IndexSetTemplates/hooks/useSelectedTemplate';
import asMock from 'helpers/mocking/AsMock';
import useProfileOptions from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfileOptions';
import useProfile from 'components/indices/IndexSetFieldTypeProfiles/hooks/useProfile';
import AppConfig from 'util/AppConfig';
import { DATA_TIERING_TYPE } from 'components/indices/data-tiering';

import IndexSetConfigurationForm from './IndexSetConfigurationForm';

const indexSet = {
  id: '62665eb0526719678ed3719f',
  title: 'Foo Title',
  description: 'test',
  can_be_default: true,
  index_prefix: 'another',
  shards: 4,
  replicas: 0,
  rotation_strategy_class: 'org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy',
  rotation_strategy: {
    type: 'org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig',
    max_docs_per_index: 20000000,
  },
  retention_strategy_class: 'org.graylog2.indexer.retention.strategies.NoopRetentionStrategy',
  retention_strategy: {
    type: 'org.graylog2.indexer.retention.strategies.NoopRetentionStrategyConfig',
    max_number_of_indices: 2147483647,
  },
  creation_date: '2022-04-25T08:41:20.497Z',
  index_analyzer: 'standard',
  index_optimization_max_num_segments: 1,
  index_optimization_disabled: true,
  field_type_refresh_interval: 5000,
  field_type_profile: null,
  index_template_type: null,
  writable: true,
  default: false,
  field_restrictions: {},
};

const retentionStrategies = [
  {
    type: 'org.graylog.plugins.archive.indexer.retention.strategies.ArchiveRetentionStrategy',
    default_config: {
      type: 'org.graylog.plugins.archive.indexer.retention.strategies.ArchiveRetentionStrategyConfig',
      max_number_of_indices: 20,
      index_action: 'CLOSE',
    },
    json_schema: {
      type: 'object',
      id: 'urn:jsonschema:org:graylog:plugins:archive:indexer:retention:strategies:ArchiveRetentionStrategyConfig',
      properties: {
        max_number_of_indices: {
          type: 'integer',
        },
        index_action: {
          type: 'string',
          enum: ['NONE', 'CLOSE', 'DELETE'],
        },
        type: {
          type: 'string',
        },
      },
    },
  },
  {
    type: 'org.graylog2.indexer.retention.strategies.NoopRetentionStrategy',
    default_config: {
      type: 'org.graylog2.indexer.retention.strategies.NoopRetentionStrategyConfig',
      max_number_of_indices: 2147483647,
    },
    json_schema: {
      type: 'object',
      id: 'urn:jsonschema:org:graylog2:indexer:retention:strategies:NoopRetentionStrategyConfig',
      properties: {
        max_number_of_indices: {
          type: 'integer',
        },
        type: {
          type: 'string',
        },
      },
    },
  },
  {
    type: 'org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy',
    default_config: {
      type: 'org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig',
      max_number_of_indices: 20,
    },
    json_schema: {
      type: 'object',
      id: 'urn:jsonschema:org:graylog2:indexer:retention:strategies:DeletionRetentionStrategyConfig',
      properties: {
        max_number_of_indices: {
          type: 'integer',
        },
        type: {
          type: 'string',
        },
      },
    },
  },
  {
    type: 'org.graylog2.indexer.retention.strategies.ClosingRetentionStrategy',
    default_config: {
      type: 'org.graylog2.indexer.retention.strategies.ClosingRetentionStrategyConfig',
      max_number_of_indices: 20,
    },
    json_schema: {
      type: 'object',
      id: 'urn:jsonschema:org:graylog2:indexer:retention:strategies:ClosingRetentionStrategyConfig',
      properties: {
        max_number_of_indices: {
          type: 'integer',
        },
        type: {
          type: 'string',
        },
      },
    },
  },
];

const retentionStrategiesContext = {
  max_index_retention_period: 'P1D',
};

const rotationStrategies = [
  {
    type: 'org.graylog2.indexer.rotation.strategies.TimeBasedRotationStrategy',
    default_config: {
      type: 'org.graylog2.indexer.rotation.strategies.TimeBasedRotationStrategyConfig',
      rotation_period: 'P1D',
      max_rotation_period: null,
      rotate_empty_index_set: false,
    },
    json_schema: {
      type: 'object',
      id: 'urn:jsonschema:org:graylog2:indexer:rotation:strategies:TimeBasedRotationStrategyConfig',
      properties: {
        rotation_period: {
          type: 'string',
        },
        max_rotation_period: {
          type: 'string',
        },
        rotate_empty_index_set: {
          type: 'boolean',
        },
        type: {
          type: 'string',
        },
      },
    },
  },
  {
    type: 'org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategy',
    default_config: {
      type: 'org.graylog2.indexer.rotation.strategies.MessageCountRotationStrategyConfig',
      max_docs_per_index: 20000000,
    },
    json_schema: {
      type: 'object',
      id: 'urn:jsonschema:org:graylog2:indexer:rotation:strategies:MessageCountRotationStrategyConfig',
      properties: {
        max_docs_per_index: {
          type: 'integer',
        },
        type: {
          type: 'string',
        },
      },
    },
  },
  {
    type: 'org.graylog2.indexer.rotation.strategies.SizeBasedRotationStrategy',
    default_config: {
      type: 'org.graylog2.indexer.rotation.strategies.SizeBasedRotationStrategyConfig',
      max_size: 1073741824,
    },
    json_schema: {
      type: 'object',
      id: 'urn:jsonschema:org:graylog2:indexer:rotation:strategies:SizeBasedRotationStrategyConfig',
      properties: {
        type: {
          type: 'string',
        },
        max_size: {
          type: 'integer',
        },
      },
    },
  },
];

const indexSetTemplateDefaults = {
  index_prefix: 'default_index_prefix',
  index_analyzer: 'default_index_analyzer',
  shards: 1,
  replicas: 1,
  index_optimization_max_num_segments: 1,
  index_optimization_disabled: true,
  field_type_refresh_interval: 30,
  field_type_refresh_interval_unit: 'minutes' as 'minutes' | 'seconds',
  use_legacy_rotation: false,
  rotation_strategy_class: 'org.graylog2.indexer.rotation.strategies.SizeBasedRotationStrategy',
  rotation_strategy_config: {
    type: 'org.graylog2.indexer.rotation.strategies.SizeBasedRotationStrategyConfig',
    max_size: 12,
  },
  retention_strategy_class: 'org.graylog2.indexer.retention.strategies.DeletionRetentionStrategy',
  retention_strategy_config: {
    type: 'org.graylog2.indexer.retention.strategies.DeletionRetentionStrategyConfig',
    max_number_of_indices: 10,
    index_action: 'foo',
  },
  data_tiering: {
    type: DATA_TIERING_TYPE.HOT_ONLY,
    index_lifetime_min: '10',
    index_lifetime_max: '30',
  },
  field_restrictions: {},
};

jest.mock('components/indices/IndexSetFieldTypeProfiles/hooks/useProfileOptions', () => jest.fn());
jest.mock('components/indices/IndexSetFieldTypeProfiles/hooks/useProfile', () => jest.fn());
jest.mock('components/indices/IndexSetTemplates/hooks/useIndexSetTemplateDefaults', () => jest.fn());
jest.mock('components/indices/IndexSetTemplates/hooks/useSelectedTemplate', () => jest.fn());

describe('IndexSetConfigurationForm', () => {
  beforeEach(() => {
    asMock(useProfileOptions).mockReturnValue({ isLoading: false, options: [], refetch: () => {} });
    asMock(useProfile).mockReturnValue({
      data: { id: null, name: null, description: null, customFieldMappings: [], indexSetIds: [] },
      isFetched: true,
      isFetching: false,
      refetch: () => {},
    });
    asMock(useIndexSetTemplateDefaults).mockReturnValue({
      loadingIndexSetTemplateDefaults: false,
      indexSetTemplateDefaults,
    });
    asMock(useSelectedIndexSetTemplate).mockReturnValue({
      selectedIndexSetTemplate: undefined,
      setSelectedIndexSetTemplate: jest.fn(),
    });
  });

  const onSave = jest.fn();
  const cancelLink = '/cancelLink';

  const SUT = (props: Partial<React.ComponentProps<typeof IndexSetConfigurationForm>>) => (
    <IndexSetConfigurationForm
      retentionStrategiesContext={retentionStrategiesContext}
      rotationStrategies={rotationStrategies}
      retentionStrategies={retentionStrategies}
      cancelLink={cancelLink}
      onUpdate={onSave}
      submitButtonText="Save"
      submitLoadingText="Saving..."
      {...props}
    />
  );

  it('Should render IndexSetConfigurationForm', async () => {
    render(<SUT indexSet={indexSet} />);

    const titleText = await screen.findByDisplayValue(/Foo Title/i);

    expect(titleText).toBeInTheDocument();
  });

  it('Should render create IndexSetConfigurationForm', async () => {
    render(<SUT create />);

    const indexAnalyzer = await screen.findByDisplayValue(/default_index_analyzer/i);

    expect(indexAnalyzer).toBeInTheDocument();
  });

  describe('Autofill index prefix', () => {
    it('Should autofill index prefix from title when creating', async () => {
      render(<SUT create />);

      const titleInput = await screen.findByRole('textbox', { name: /title/i });
      const prefixInput = await screen.findByRole('textbox', { name: /index prefix/i });

      await userEvent.type(titleInput, 'My Index');

      await waitFor(() => {
        expect(prefixInput).toHaveValue('my-index');
      });
    });

    it('Should stop autofill when user clears the prefix and enters a custom one', async () => {
      render(<SUT create />);

      const titleInput = await screen.findByRole('textbox', { name: /title/i });
      const prefixInput = await screen.findByRole('textbox', { name: /index prefix/i });

      await userEvent.type(titleInput, 'My Index');

      await waitFor(() => {
        expect(prefixInput).toHaveValue('my-index');
      });

      await userEvent.clear(prefixInput);
      await userEvent.type(prefixInput, 'custom-prefix');

      // Change the title again - autofill should be disabled now
      await userEvent.type(titleInput, ' Updated');

      // Prefix should remain as the manually modified value
      await waitFor(() => {
        expect(prefixInput).toHaveValue('custom-prefix');
      });
    });

    it('Should stop autofill when user appends to the prefix without clearing', async () => {
      render(<SUT create />);

      const titleInput = await screen.findByRole('textbox', { name: /title/i });
      const prefixInput = await screen.findByRole('textbox', { name: /index prefix/i });

      // Type title which autofills the prefix
      await userEvent.type(titleInput, 'My Index');

      await waitFor(() => {
        expect(prefixInput).toHaveValue('my-index');
      });

      // Now append to the autofilled prefix without clearing
      await userEvent.type(prefixInput, '-suffix');

      await waitFor(() => {
        expect(prefixInput).toHaveValue('my-index-suffix');
      });

      // Change the title again - autofill should be disabled now
      await userEvent.type(titleInput, ' Updated');

      // Prefix should remain as the manually modified value
      await waitFor(() => {
        expect(prefixInput).toHaveValue('my-index-suffix');
      });
    });

    it('Should not autofill when editing existing index set', async () => {
      render(<SUT indexSet={indexSet} />);

      const titleInput = await screen.findByRole('textbox', { name: /title/i });

      // Clear existing title and type new one
      await userEvent.clear(titleInput);
      await userEvent.type(titleInput, 'New Title');

      // Prefix field should not exist in edit mode (it's read-only after creation)
      await waitFor(() => {
        expect(screen.queryByRole('textbox', { name: /index prefix/i })).not.toBeInTheDocument();
      });
    });
  });

  describe('Selected index set template', () => {
    const selectedTemplate = {
      id: 'template-id',
      title: 'Template',
      description: 'Template description',
      built_in: false,
      default: false,
      enabled: true,
      disabled_reason: '',
      index_set_config: {
        ...indexSetTemplateDefaults,
        shards: 7,
        field_limit: 1200,
        use_legacy_rotation: true,
        data_tiering: { type: DATA_TIERING_TYPE.HOT_ONLY, index_lifetime_min: '1', index_lifetime_max: '2' },
      },
    };

    it('Should prefill the form from the template without changing the template', async () => {
      const templateDataTiering = { ...selectedTemplate.index_set_config.data_tiering };

      asMock(useSelectedIndexSetTemplate).mockReturnValue({
        selectedIndexSetTemplate: selectedTemplate as never,
        setSelectedIndexSetTemplate: jest.fn(),
      });

      render(<SUT create />);

      expect(await screen.findByRole('spinbutton', { name: /index shards/i })).toHaveValue(7);
      expect(screen.getByRole('spinbutton', { name: /max field count/i })).toHaveValue(1200);
      expect(selectedTemplate.index_set_config.data_tiering).toEqual(templateDataTiering);
    });
  });

  describe('Max field count and dynamic mapping', () => {
    it('Should show the default as placeholder and disable the toggle without a profile', async () => {
      render(<SUT indexSet={indexSet} />);

      const limitInput = await screen.findByRole('spinbutton', { name: /max field count/i });

      expect(limitInput).toHaveAttribute('placeholder', '1000 (default)');
      expect(await screen.findByLabelText(/disable dynamic index mapping/i)).toBeDisabled();
      expect(screen.getByText(/assign a field type profile to this index set first/i)).toBeInTheDocument();
    });

    it('Should take the limit of the default template when creating without a selected template', async () => {
      asMock(useIndexSetTemplateDefaults).mockReturnValue({
        loadingIndexSetTemplateDefaults: false,
        indexSetTemplateDefaults: { ...indexSetTemplateDefaults, field_limit: 1500 },
      });

      render(<SUT create />);

      expect(await screen.findByRole('spinbutton', { name: /max field count/i })).toHaveValue(1500);
    });

    it('Should keep an unset limit empty when editing, even if the default template has one', async () => {
      asMock(useIndexSetTemplateDefaults).mockReturnValue({
        loadingIndexSetTemplateDefaults: false,
        indexSetTemplateDefaults: { ...indexSetTemplateDefaults, field_limit: 1500 },
      });

      render(<SUT indexSet={{ ...indexSet, field_limit: null }} />);

      expect(await screen.findByRole('spinbutton', { name: /max field count/i })).toHaveValue(null);
    });

    it('Should enable the toggle when a profile is assigned', async () => {
      render(<SUT indexSet={{ ...indexSet, field_type_profile: 'profile-id' }} />);

      expect(await screen.findByLabelText(/disable dynamic index mapping/i)).toBeEnabled();
    });

    it('Should warn when the assigned profile holds as many mappings as the limit', async () => {
      asMock(useProfile).mockReturnValue({
        data: {
          id: 'profile-id',
          name: 'Profile',
          description: '',
          customFieldMappings: Array.from({ length: 1000 }, (_, i) => ({ field: `field_${i}`, type: 'string' })),
          indexSetIds: [],
        },
        isFetched: true,
        isFetching: false,
        refetch: () => {},
      });

      render(<SUT indexSet={{ ...indexSet, field_type_profile: 'profile-id' }} />);

      expect(await screen.findByText(/as many as or more than the limit of 1000 fields/i)).toBeInTheDocument();
    });

    it('Should not show the settings for event index sets', async () => {
      render(<SUT indexSet={{ ...indexSet, index_template_type: 'events' }} />);

      await screen.findByDisplayValue(/Foo Title/i);

      expect(screen.queryByRole('spinbutton', { name: /max field count/i })).not.toBeInTheDocument();
      expect(screen.queryByLabelText(/disable dynamic index mapping/i)).not.toBeInTheDocument();
    });

    it('Should show the max field count and an enabled toggle for Illuminate index sets', async () => {
      render(<SUT indexSet={{ ...indexSet, index_template_type: 'illuminate_content' }} />);

      expect(await screen.findByRole('spinbutton', { name: /max field count/i })).toBeInTheDocument();
      expect(await screen.findByLabelText(/disable dynamic index mapping/i)).toBeEnabled();
      expect(screen.getByText(/fields first seen later are not searchable/i)).toBeInTheDocument();
    });

    it('Should show how many fields were kept on Illuminate index sets with dynamic mapping off', async () => {
      render(
        <SUT
          indexSet={{
            ...indexSet,
            index_template_type: 'illuminate_content',
            dynamic_mapping_disabled: true,
            kept_fields_count: 42,
          }}
        />,
      );

      expect(await screen.findByText(/42 fields were kept from the write index/i)).toBeInTheDocument();
    });

    it('Should warn when no fields were kept on Illuminate index sets with dynamic mapping off', async () => {
      render(
        <SUT
          indexSet={{
            ...indexSet,
            index_template_type: 'illuminate_content',
            dynamic_mapping_disabled: true,
            kept_fields_count: null,
          }}
        />,
      );

      expect(
        await screen.findByText(/only the fields the illuminate package lists stay searchable/i),
      ).toBeInTheDocument();
    });

    it('Should hide the max field count but keep the toggle on Cloud', async () => {
      const isCloud = jest.spyOn(AppConfig, 'isCloud').mockReturnValue(true);

      render(<SUT indexSet={{ ...indexSet, field_type_profile: 'profile-id' }} />);

      expect(await screen.findByLabelText(/disable dynamic index mapping/i)).toBeInTheDocument();
      expect(screen.queryByRole('spinbutton', { name: /max field count/i })).not.toBeInTheDocument();

      isCloud.mockRestore();
    });

    it('Should keep dynamic mapping on when the confirmation is cancelled', async () => {
      render(<SUT indexSet={{ ...indexSet, field_type_profile: 'profile-id' }} />);

      const toggle = await screen.findByLabelText(/disable dynamic index mapping/i);
      await userEvent.click(toggle);

      const dialog = await screen.findByRole('dialog');
      await userEvent.click(within(dialog).getByRole('button', { name: /cancel/i }));

      await waitFor(() => {
        expect(screen.queryByText('Disable dynamic index mapping?')).not.toBeInTheDocument();
      });

      expect(toggle).not.toBeChecked();
    });

    it('Should disable dynamic mapping only after the confirmation', async () => {
      render(<SUT indexSet={{ ...indexSet, field_type_profile: 'profile-id' }} />);

      const toggle = await screen.findByLabelText(/disable dynamic index mapping/i);
      await userEvent.click(toggle);

      expect(toggle).not.toBeChecked();

      const dialog = await screen.findByRole('dialog');
      await userEvent.click(within(dialog).getByRole('button', { name: /disable dynamic mapping/i }));

      await waitFor(() => {
        expect(toggle).toBeChecked();
      });

      await userEvent.click(await screen.findByRole('button', { name: /save/i }));

      await waitFor(() => {
        expect(onSave).toHaveBeenCalledWith(expect.objectContaining({ dynamic_mapping_disabled: true }));
      });
    });

    it('Should turn dynamic mapping back on without a confirmation', async () => {
      render(<SUT indexSet={{ ...indexSet, field_type_profile: 'profile-id', dynamic_mapping_disabled: true }} />);

      const toggle = await screen.findByLabelText(/disable dynamic index mapping/i);
      await userEvent.click(toggle);

      await waitFor(() => {
        expect(toggle).not.toBeChecked();
      });

      expect(screen.queryByText('Disable dynamic index mapping?')).not.toBeInTheDocument();
    });

    it('Should show the backend error on the field', async () => {
      onSave.mockRejectedValueOnce({
        additional: { body: [{ path: 'field_limit', message: 'must be between 1000 and 2000' }] },
      });

      render(<SUT indexSet={indexSet} />);

      await userEvent.type(await screen.findByRole('spinbutton', { name: /max field count/i }), '5000');
      await userEvent.click(await screen.findByRole('button', { name: /save/i }));

      expect(await screen.findByText(/must be between 1000 and 2000/i)).toBeInTheDocument();
    });
  });
});
