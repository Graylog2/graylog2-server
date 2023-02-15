import React, { useState } from 'react';
import styled from 'styled-components';

import OverlayDropdownButton from 'components/common/OverlayDropdownButton';
import MenuItem from 'components/bootstrap/MenuItem';
import { Icon } from 'components/common';
import type { Attributes } from 'stores/PaginationTypes';

const Container = styled.div`
  margin-left: 5px;
`;

type Props = {
  filterableAttributes: Attributes,
  onSubmit: (attributeId: string, filter: { value: string, title: string }) => void,
}

const CreateFilterDropdown = ({ filterableAttributes, onSubmit }: Props) => {
  const [selectedFilterId, setSelectedFilterId] = useState<string>();
  const selectedFilter = filterableAttributes.find(({ id }) => id === selectedFilterId);
  const onToggleDropdown = () => setSelectedFilterId(undefined);

  return (
    <Container>
      <OverlayDropdownButton title={<Icon name="plus" />}
                             bsSize="small"
                             onToggle={onToggleDropdown}
                             closeOnSelect={false}
                             dropdownZIndex={1000}>
        {!selectedFilterId && (
          <>
            <MenuItem header>Create Filter</MenuItem>
            {filterableAttributes.map(({ id, title }) => (
              <MenuItem onSelect={() => setSelectedFilterId(id)}>{title}</MenuItem>
            ))}
          </>
        )}

        {selectedFilterId && (
          <>
            <MenuItem header>Create {selectedFilter.title} Filter</MenuItem>
            {selectedFilter.type === 'boolean' && (
              <>
                {selectedFilter.filter_options.map(({ title, value }) => (
                  <MenuItem onSelect={() => onSubmit(selectedFilter.id, { value, title })}>{title}</MenuItem>
                ))}
              </>
            )}
          </>
        )}
      </OverlayDropdownButton>
    </Container>
  );
};

export default CreateFilterDropdown;
