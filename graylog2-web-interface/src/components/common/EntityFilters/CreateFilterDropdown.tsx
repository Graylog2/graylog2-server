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
import type { Attributes } from 'stores/PaginationTypes';
import type { Filters, Filter } from 'components/common/EntityFilters/types';
import FilterConfiguration from 'components/common/EntityFilters/FilterConfiguration';

const Container = styled.div`
  margin-left: 5px;
`;

const AttributeSelect = ({
  attributes,
  setSelectedAttributeId,
  activeFilters,
}: {
  attributes: Attributes,
  activeFilters: Filters | undefined,
  setSelectedAttributeId: React.Dispatch<React.SetStateAction<string>>
}) => (
  <>
    <MenuItem header>Create Filter</MenuItem>
    {attributes.map(({ id, title, type }) => {
      const hasActiveFilter = !!activeFilters?.get(id)?.length;
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

type Props = {
  filterableAttributes: Attributes,
  activeFilters: Filters | undefined,
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
                             dropdownMinWidth={120}
                             dropdownZIndex={1000}>
        {({ toggleDropdown }) => {
          const _onCreateFilter = (filter: { title: string, value: string }, closeDropdown = true) => {
            if (closeDropdown) {
              toggleDropdown();
            }

            onCreateFilter(selectedAttributeId, { value: filter.value, title: filter.title });
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
                                 allActiveFilters={activeFilters}
                                 attribute={selectedAttribute}
                                 filterValueRenderer={filterValueRenderers?.[selectedAttributeId]} />
          );
        }}
      </OverlayDropdownButton>
    </Container>
  );
};

export default CreateFilterDropdown;
