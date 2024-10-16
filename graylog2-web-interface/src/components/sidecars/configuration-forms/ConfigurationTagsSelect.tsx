import React from 'react';

import MultiSelect from 'components/common/MultiSelect';

type Props = {
  tags: string[],
  availableTags: { name: string }[],
  onChange: (tagsAsString: string) => void,
};

const ConfigurationTagsSelect = ({
  tags,
  availableTags,
  onChange,
}: Props) => {
  const tagsValue = tags.join(',');
  const tagsOptions = availableTags.map((tag) => ({ value: tag.name, label: tag.name }));

  return (
    <MultiSelect options={tagsOptions}
                 value={tagsValue}
                 onChange={onChange}
                 placeholder="Choose tags..."
                 allowCreate />
  );
};

export default ConfigurationTagsSelect;
