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
import { useMemo } from 'react';
import * as React from 'react';
import styled from 'styled-components';
import { components as ReactSelectComponents } from 'react-select';

import {Select, Icon} from 'components/common';
import { defaultCompare } from 'logic/DefaultCompare';

type StreamsAndCategoriesOption = {
  id: string;
  label: string;
  value: string;
  type: 'stream' | 'category';
};

export type StreamsAndCategoriesSelection = {
  streams: string[];
  categories: string[];
};

const StyledIcon = styled(Icon)`
  width: 24px;
  vertical-align: middle;
`;

const renderOption = (option: StreamsAndCategoriesOption) =>
  option.type === 'stream' ? (
    <>{option.label}</>
  ) : (
    <>
      <StyledIcon name={'stacks'} size="xs" />{option.label}
    </>
  );

const MultiValueLabel = (props: React.ComponentProps<typeof ReactSelectComponents.MultiValueLabel>) => (
  <ReactSelectComponents.MultiValueLabel {...props}>
    {renderOption(props.data as StreamsAndCategoriesOption)}
  </ReactSelectComponents.MultiValueLabel>
);

type StreamsAndCategoriesFilterProps = Omit<React.ComponentProps<typeof Select>, 'options'> & {
  onChange: (value: StreamsAndCategoriesSelection) => void;
  streams: Array<{ id: string; title: string; categories: string[] }>;
  streamCategories?: string[] | undefined;
  showStreams?: boolean;
  showCategories?: boolean;
  grouping?: boolean;
};

const StreamsAndCategoriesFilter = ( {
                                       id,
                                       onChange,
                                       value,
                                       streams,
                                       streamCategories = undefined,
                                       required = true,
                                       showStreams = true,
                                       showCategories = true,
                                       grouping = true,
                                       multi = true,
                                       ...rest }: StreamsAndCategoriesFilterProps) => {
  const options = useMemo(() => {
    const sortedCategories =
      showCategories && multi
        ? [...new Set<string>(streams.flatMap((stream) => streamCategories ?? stream?.categories ?? []))]
            .map((category) => ({ id: category, label: category, value: 'category_' + category, type: 'category' }))
            .sort((a: StreamsAndCategoriesOption, b: StreamsAndCategoriesOption) => defaultCompare(a.label, b.label))
        : [];
    const sortedStreams = showStreams
      ? streams
          .map((stream: { id: string; title: string }) => ({
            id: stream.id,
            value: 'stream_' + stream.id,
            label: stream.title,
            type: 'stream',
          }))
          .sort((a: StreamsAndCategoriesOption, b: StreamsAndCategoriesOption) => defaultCompare(a.label, b.label))
      : [];

    return grouping
      ? [...sortedCategories, ...sortedStreams]
      : [...sortedCategories, ...sortedStreams].sort((a: StreamsAndCategoriesOption, b: StreamsAndCategoriesOption) =>
          defaultCompare(a.label, b.label),
        );
  }, [grouping, multi, showCategories, showStreams, streams, streamCategories]);

  if (!options || options.length === 0) return null;

  const change = (selected: string) => {
    onChange({
      streams: selected
        .split(',')
        .filter((v) => v.startsWith('stream_'))
        .map((s) => s.substring('stream_'.length)),
      categories: selected
        .split(',')
        .filter((v) => v.startsWith('category_'))
        .map((c) => c.substring('category_'.length)),
    });
  };

  return (
    <Select
      {...rest}
      id={id}
      onChange={change}
      options={options}
      optionRenderer={renderOption}
      valueRenderer={renderOption}
      components={{ MultiValueLabel }}
      value={value}
      required={required}
      multi={multi}
      />
  );
};

export default StreamsAndCategoriesFilter;
