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
import { mount } from 'wrappedEnzyme';
import 'helpers/mocking/react-dom_mock';

import ContentPackConstraints from 'components/content-packs/ContentPackConstraints';

describe('<ContentPackConstraints />', () => {
  it('should render with new constraints without forced fulfillment', () => {
    const constraints = [{
      type: 'server-version',
      version: '>=3.0.0-alpha.2+af8d8e0',
    }, {
      plugin: 'org.graylog.plugins.threatintel.ThreatIntelPlugin',
      type: 'plugin-version',
      version: '>=3.0.0-alpha.2',
    }];
    const wrapper = mount(<ContentPackConstraints constraints={constraints} />);

    expect(wrapper).toExist();
  });

  it('should render with new constraints with forced fulfillment', () => {
    const constraints = [{
      type: 'server-version',
      version: '>=3.0.0-alpha.2+af8d8e0',
    }, {
      plugin: 'org.graylog.plugins.threatintel.ThreatIntelPlugin',
      type: 'plugin-version',
      version: '>=3.0.0-alpha.2',
    }];
    const wrapper = mount(<ContentPackConstraints constraints={constraints} isFulfilled />);

    expect(wrapper).toExist();
  });

  it('should render with created constraints', () => {
    const constraints = [
      {
        constraint: {
          type: 'server-version',
          version: '>=3.0.0-alpha.2+af8d8e0',
        },
        fulfilled: true,
      }, {
        constraint: {
          plugin: 'org.graylog.plugins.threatintel.ThreatIntelPlugin',
          type: 'plugin-version',
          version: '>=3.0.0-alpha.2',
        },
        fulfilled: false,
      }];
    const wrapper = mount(<ContentPackConstraints constraints={constraints} />);

    expect(wrapper).toExist();
  });
});
