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
import PropTypes from 'prop-types';
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
  const tagsOptions = availableTags.map((tag) => {
    return { value: tag.name, label: tag.name };
  });

  return (
    <MultiSelect options={tagsOptions}
                 value={tagsValue}
                 onChange={onChange}
                 placeholder="Choose tags..."
                 allowCreate />
  );
};

ConfigurationTagsSelect.propTypes = {
  tags: PropTypes.arrayOf(PropTypes.string),
  availableTags: PropTypes.array.isRequired,
  onChange: PropTypes.func.isRequired,
};

ConfigurationTagsSelect.defaultProps = {
  tags: [],
};

export default ConfigurationTagsSelect;
