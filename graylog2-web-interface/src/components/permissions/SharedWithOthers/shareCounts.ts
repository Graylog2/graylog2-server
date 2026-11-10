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
import mockedPermissions from 'logic/permissions/mocked';
import StringUtils from 'util/StringUtils';

export type Capability = 'own' | 'manage' | 'view';

export type ShareSummary = Record<Capability, number>;

export type GranteeShare = {
  grantee: { id: string; type: string; title: string };
  capability: Capability;
};

const CAPABILITY_ORDER: Array<Capability> = ['own', 'manage', 'view'];

export const capabilityTitle = (capability: Capability) => mockedPermissions.availableCapabilities[capability];

/**
 * Formats the grantee counts of an entity per access level, e.g. "1 Owner, 3 Viewers". Leaves out levels without grantees.
 */
export const formatShareCounts = (summary: ShareSummary) =>
  CAPABILITY_ORDER.filter((capability) => summary[capability] > 0)
    .map((capability) => {
      const title = capabilityTitle(capability);

      return `${summary[capability]} ${StringUtils.pluralize(summary[capability], title, `${title}s`)}`;
    })
    .join(', ');

/**
 * Sorts grantees by access level (Owner, Manager, Viewer), then by name.
 */
export const sortGranteeShares = (shares: Array<GranteeShare>) =>
  [...shares].sort(
    (a, b) =>
      CAPABILITY_ORDER.indexOf(a.capability) - CAPABILITY_ORDER.indexOf(b.capability) ||
      a.grantee.title.localeCompare(b.grantee.title),
  );
