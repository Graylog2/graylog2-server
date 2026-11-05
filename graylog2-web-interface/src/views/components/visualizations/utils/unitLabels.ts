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
import capitalize from 'lodash/capitalize';

import i18n from 'i18n';

// The names of the units come from `supported_units.json`, e.g. "kilobyte" or "percent(1..100)".
const toKey = (value: string) => value.replace(/[^a-z0-9]+/gi, '_').replace(/^_+|_+$/g, '');

export const unitTypeLabel = (unitType: string) =>
  i18n.t(`wizard_units.unit_type_labels.${unitType}`, { ns: 'aggregation', defaultValue: capitalize(unitType) });

export const unitNameLabel = (unitName: string) =>
  i18n.t(`wizard_units.unit_labels.${toKey(unitName)}`, { ns: 'aggregation', defaultValue: capitalize(unitName) });
