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
import { render, screen } from 'wrappedTestingLibrary';

import AppConfig from 'util/AppConfig';
import IndexSetTemplateCard from 'components/indices/IndexSetTemplates/IndexSetTemplateCard';
import type { IndexSetTemplate } from 'components/indices/IndexSetTemplates/types';

const buildTemplate = (config: Partial<IndexSetTemplate['index_set_config']> = {}): IndexSetTemplate => ({
  id: 'template-id',
  title: 'Test template',
  description: 'Template description',
  built_in: false,
  default: false,
  enabled: true,
  disabled_reason: '',
  index_set_config: {
    use_legacy_rotation: true,
    data_tiering: {},
    ...config,
  } as IndexSetTemplate['index_set_config'],
});

const renderCard = (template: IndexSetTemplate) =>
  render(<IndexSetTemplateCard template={template} handleCardClick={() => {}} isSelected={false} />);

describe('IndexSetTemplateCard', () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('shows the default max field count and enabled dynamic mapping for templates without the settings', async () => {
    renderCard(buildTemplate());

    expect(await screen.findByText('1000 (default)')).toBeInTheDocument();
    expect(screen.getByText('Enabled')).toBeInTheDocument();
  });

  it('shows the configured max field count and disabled dynamic mapping', async () => {
    renderCard(buildTemplate({ field_limit: 1500, dynamic_mapping_disabled: true }));

    expect(await screen.findByText('1500')).toBeInTheDocument();
    expect(screen.getByText('Disabled')).toBeInTheDocument();
  });

  it('hides the max field count on Graylog Cloud but keeps dynamic mapping', async () => {
    jest.spyOn(AppConfig, 'isCloud').mockReturnValue(true);

    renderCard(buildTemplate({ field_limit: 1500 }));

    expect(await screen.findByText('Dynamic index mapping:')).toBeInTheDocument();
    expect(screen.queryByText('Max field count:')).not.toBeInTheDocument();
  });
});
