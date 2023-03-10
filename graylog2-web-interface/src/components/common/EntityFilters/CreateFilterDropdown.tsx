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
import React, { useState } from 'react';
import styled from 'styled-components';

import OverlayDropdownButton from 'components/common/OverlayDropdownButton';
import MenuItem from 'components/bootstrap/MenuItem';
import { HoverForHelp, Icon } from 'components/common';
import type { Attribute, Attributes } from 'stores/PaginationTypes';
import type { Filters, Filter } from 'components/common/EntityFilters/types';

import SuggestionsList from './configuration/SuggestionsList';
import StaticOptionsList from './configuration/StaticOptionsList';

const Container = styled.div`
  margin-left: 5px;
`;

const AttributeSelect = ({
  attributes,
  setSelectedAttributeId,
  activeFilters,
}: {
  attributes: Attributes,
  activeFilters: Filters,
  setSelectedAttributeId: React.Dispatch<React.SetStateAction<string>>
}) => (
  <>
    <MenuItem header>Create Filter</MenuItem>
    {attributes.map(({ id, title, type }) => {
      const hasActiveFilter = !!activeFilters[id]?.length;
      const disabled = type === 'BOOLEAN' ? hasActiveFilter : false;

      return (
        <MenuItem onSelect={() => setSelectedAttributeId(id)}
                  key={`${title}-filter`}
                  disabled={disabled}>
          {title}
          {(type === 'BOOLEAN' && disabled) && (
            <HoverForHelp displayLeftMargin>
              You can only create one filter for a boolean attribute.<br />
              If you want to change the filter value, you can update the existing one.
            </HoverForHelp>
          )}
        </MenuItem>
      );
    })}
  </>
);

const FilterConfiguration = ({
  attribute,
  onSubmit,
  filterValueRenderer,
}: {
  attribute: Attribute,
  filterValueRenderer: (value: Filter['value'], title: string) => React.ReactNode | undefined,
  onSubmit: (filter: Filter) => void,
}) => (
  <>
    <MenuItem header>Create {attribute.title} Filter</MenuItem>
    {attribute.filter_options && (
      <StaticOptionsList attribute={attribute} filterValueRenderer={filterValueRenderer} onSubmit={onSubmit} />
    )}
    {!attribute.filter_options?.length && (
      <SuggestionsList attribute={attribute} filterValueRenderer={filterValueRenderer} onSubmit={onSubmit} />
    )}
  </>
);

type Props = {
  filterableAttributes: Attributes,
  activeFilters: Filters,
  onCreateFilter: (attributeId: string, filter: Filter) => void,
  filterValueRenderers: { [attributeId: string]: (value: Filter['value'], title: string) => React.ReactNode } | undefined;
}

const CreateFilterDropdown = ({ filterableAttributes, filterValueRenderers, onCreateFilter, activeFilters }: Props) => {
  const [selectedAttributeId, setSelectedAttributeId] = useState<string>();
  const selectedAttribute = filterableAttributes.find(({ id }) => id === selectedAttributeId);
  const onToggleDropdown = () => setSelectedAttributeId(undefined);

  return (
    <Container>
      <OverlayDropdownButton title={<Icon name="plus" />}
                             bsSize="small"
                             buttonTitle="Create Filter"
                             onToggle={onToggleDropdown}
                             closeOnSelect={false}
                             dropdownZIndex={1000}>
        {({ toggleDropdown }) => {
          const _onCreateFilter = (filter: Filter) => {
            toggleDropdown();
            onCreateFilter(selectedAttributeId, filter);
          };

          if (!selectedAttributeId) {
            return (
              <AttributeSelect attributes={filterableAttributes}
                               setSelectedAttributeId={setSelectedAttributeId}
                               activeFilters={activeFilters} />
            );
          }

          return (
            <FilterConfiguration onSubmit={_onCreateFilter}
                                 attribute={selectedAttribute}
                                 filterValueRenderer={filterValueRenderers?.[selectedAttributeId]} />
          );
        }}
      </OverlayDropdownButton>
    </Container>
  );
};

export default CreateFilterDropdown;
