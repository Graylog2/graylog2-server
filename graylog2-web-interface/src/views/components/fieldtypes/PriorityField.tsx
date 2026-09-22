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

import EventDefinitionPriorityEnum from 'logic/alerts/EventDefinitionPriorityEnum';
import StringUtils from 'util/StringUtils';

type Props = {
  value: string | number;
};

const PriorityField = ({ value }: Props) => {
  const priorityName = EventDefinitionPriorityEnum.properties[value]?.name;

  return <span title={String(value)}>{priorityName ? StringUtils.capitalizeFirstLetter(priorityName) : value}</span>;
};

export default PriorityField;
