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

import type { CoreSystemConfiguration } from 'views/types';

import buildSections from './buildSections';
import FeatureSection from './FeatureSection';
import { passwordFeatureSummary } from './__tests__/fixtures';

const SearchesConfig = () => null;
const PasswordConfig = () => null;
const LegacyConfig = () => null;
const Section = () => null;

const core: Array<CoreSystemConfiguration> = [
  {
    name: 'Search',
    feature: 'search',
    SectionComponent: Section,
    props: { ConfigurationComponent: SearchesConfig, title: 'Search' },
  },
  {
    name: 'Password Policy',
    feature: 'password_policy',
    SectionComponent: Section,
    props: { ConfigurationComponent: PasswordConfig, title: 'Password Policy' },
  },
  { name: 'Legacy', SectionComponent: Section, props: { ConfigurationComponent: LegacyConfig, title: 'Legacy' } },
  {
    name: 'Secret',
    feature: 'search',
    permissions: ['urlallowlist:read'],
    SectionComponent: Section,
    props: { ConfigurationComponent: LegacyConfig, title: 'Secret' },
  },
];

const features = [
  { id: 'search', title: 'Search', tunableCount: 0 },
  { id: 'events', title: 'Events', tunableCount: 0 },
  passwordFeatureSummary,
];

describe('buildSections', () => {
  it('creates one section per feature with content, then unassigned core entries', () => {
    const sections = buildSections({
      features,
      coreConfigurations: core,
      isPermitted: () => false,
    });

    expect(sections.map(({ name }) => name)).toEqual(['Search', 'Password Policy', 'Legacy']);
    expect(sections[0].SectionComponent).toBe(FeatureSection);
    expect(sections[1].props).toEqual({
      feature: passwordFeatureSummary,
      coreConfigurations: [core[1]],
    });
    expect(sections[2]).toBe(core[2]);
  });

  it('includes permitted entries only', () => {
    const sections = buildSections({
      features,
      coreConfigurations: core,
      isPermitted: () => true,
    });

    expect(sections[0].props).toEqual(expect.objectContaining({ coreConfigurations: [core[0], core[3]] }));
  });

  it('keeps the existing sections without features', () => {
    const sections = buildSections({
      features: [],
      coreConfigurations: core,
      isPermitted: () => true,
    });

    expect(sections).toEqual(core);
  });
});
