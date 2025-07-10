
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
import usePluginEntities from 'hooks/usePluginEntities';
import { ColumnRenderersByAttribute, EntityBase, ExpandedSectionRenderer } from 'components/common/EntityDataTable/types';

const  usePluggableEntityTableElements = <T extends EntityBase>(_entity: T, entityType: string): {
  pluggableColumnRenderers:  ColumnRenderersByAttribute<T>,
  pluggableAttributes: {
    attributeNames: string[];
    attributes: Array<{ id: string; title: string }>;
  };
  pluggableExpandedSections?: {[sectionName: string]: ExpandedSectionRenderer<T>};
} => {
  const pluginTableElements = usePluginEntities('components.shared.entityTableElements');

  const tableElements = pluginTableElements.filter((action) =>
    (action.useCondition ? !!action.useCondition() : true));
  const pluggableColumnRenderers = tableElements.reduce((acc, curr) =>
    ({ ...acc, ...curr.getColumnRenderer(entityType) }), {});
  const pluggableAttributes = tableElements.reduce((acc, curr) =>
    ([ ...acc, ...curr.attributes ]), []);
  const pluggableAttributeNames = tableElements.reduce((acc, curr) =>
    ([ ...acc, curr.attributeName ]), []);
  const pluggableExpandedSections = tableElements.reduce((acc, curr) =>
    ({ ...acc, ...curr.expandedSection(entityType) }), {});

  return {
    pluggableColumnRenderers,
    pluggableAttributes: {
      attributeNames: pluggableAttributeNames,
      attributes: pluggableAttributes,
    },
    pluggableExpandedSections,
  };
}

export default usePluggableEntityTableElements;
