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

import getUnitTextLabel from 'views/components/visualizations/utils/getUnitTextLabel';
import { formatNumber } from 'util/NumberFormatting';

const formatValueWithUnitLabel = (value: number | string, abbrev: string) => `${formatNumber(Number(value), { minimumDigits: 1 })} ${getUnitTextLabel(abbrev)}`;

export default formatValueWithUnitLabel;
