import React, { useState } from 'react';
import styled from 'styled-components';

import OverlayDropdownButton from 'components/common/OverlayDropdownButton';
import MenuItem from 'components/bootstrap/MenuItem';
import { Icon } from 'components/common';
import type { Attribute, Attributes } from 'stores/PaginationTypes';
import generateId from 'logic/generateId';
import type { Filters } from 'components/common/EntityFilters/types';

const Container = styled.div`
  margin-left: 5px;
`;

const FilterTypeSelect = ({
  attributes,
  setSelectedAttributeId,
}: {
  attributes: Attributes,
  setSelectedAttributeId: React.Dispatch<React.SetStateAction<string>>
}) => (
  <>
    <MenuItem header>Create Filter</MenuItem>
    {attributes.map(({ id, title }) => (
      <MenuItem onSelect={() => setSelectedAttributeId(id)} key={`${title}-filter`}>
        {title}
      </MenuItem>
    ))}
  </>
);

const FilterConfiguration = ({
  attribute,
  onSubmit,
  filterValueRenderer,
}: {
  attribute: Attribute,
  filterValueRenderer: (value: unknown, title: string) => React.ReactNode | undefined,
  onSubmit: (filter: { value: string, title: string, id: string }) => void,
}) => (
  <>
    <MenuItem header>Create {attribute.title} Filter</MenuItem>
    {attribute.type === 'boolean' && (
      <>
        {attribute.filter_options.map(({ title, value }) => (
          <MenuItem onSelect={() => onSubmit({ value, title, id: generateId() })} key={`filter-value-${title}`}>
            {filterValueRenderer ? filterValueRenderer(value, title) : title}
          </MenuItem>
        ))}
      </>
    )}
  </>
);

type Props = {
  filterableAttributes: Attributes,
  activeFilters: Filters,
  onCreateFilter: (attributeId: string, filter: { value: string, title: string, id: string }) => void,
  filterValueRenderers: { [attributeId: string]: (value: unknown, title: string) => React.ReactNode } | undefined;
}

const CreateFilterDropdown = ({ filterableAttributes, filterValueRenderers, onCreateFilter, activeFilters }: Props) => {
  const [selectedAttributeId, setSelectedAttributeId] = useState<string>();
  const selectedAttribute = filterableAttributes.find(({ id }) => id === selectedAttributeId);
  const onToggleDropdown = () => setSelectedAttributeId(undefined);

  return (
    <Container>
      <OverlayDropdownButton title={<Icon name="plus" />}
                             bsSize="small"
                             onToggle={onToggleDropdown}
                             closeOnSelect={false}
                             dropdownZIndex={1000}>
        {({ toggleDropdown }) => {
          const _onCreateFilter = (filter: { value: string, title: string, id: string }) => {
            toggleDropdown();
            onCreateFilter(selectedAttributeId, filter);
          };

          if (!selectedAttributeId) {
            return (
              <FilterTypeSelect attributes={filterableAttributes} setSelectedAttributeId={setSelectedAttributeId} />
            );
          }

          return (
            <FilterConfiguration onSubmit={_onCreateFilter}
                                 attribute={selectedAttribute}
                                 filterValueRenderer={filterValueRenderers[selectedAttributeId]} />
          );
        }}
      </OverlayDropdownButton>
    </Container>
  );
};

export default CreateFilterDropdown;
