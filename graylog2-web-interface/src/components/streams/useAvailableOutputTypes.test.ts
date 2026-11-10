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

import {
  getOutputTypeDefinition,
  getRequestedOutputConfiguration,
  type AvailableOutputTypes,
} from 'components/streams/useAvailableOutputTypes';
import type { ConfigurationField } from 'components/configurationforms';

describe('useAvailableOutputTypes helpers', () => {
  const hostField: ConfigurationField = {
    type: 'text',
    human_name: 'Host',
    additional_info: {},
    attributes: [],
    default_value: '',
    description: 'Host to connect to',
    is_encrypted: false,
    is_optional: false,
    position: 0,
  };

  const outputTypes: AvailableOutputTypes = {
    'enterprise-output': {
      type: 'enterprise-output',
      name: 'Enterprise output',
      human_name: 'Enterprise output',
      link_to_docs: '',
      requested_configuration: {
        host: hostField,
      },
    },
  };

  it('returns output definition for a known output type', () => {
    expect(getOutputTypeDefinition(outputTypes, 'enterprise-output')).toEqual(outputTypes['enterprise-output']);
  });

  it('returns requested configuration from the flat output types map', () => {
    expect(getRequestedOutputConfiguration(outputTypes, 'enterprise-output')).toEqual(
      outputTypes['enterprise-output'].requested_configuration,
    );
  });

  it('returns undefined for unknown type or missing map', () => {
    expect(getOutputTypeDefinition(outputTypes, 'missing-output')).toBeUndefined();
    expect(getRequestedOutputConfiguration(undefined, 'enterprise-output')).toBeUndefined();
  });
});
