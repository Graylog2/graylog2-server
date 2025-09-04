import * as React from 'react';
import styled, { css } from 'styled-components';
import { Accordion } from '@mantine/core';

import { Checkbox } from 'components/bootstrap';

const Subheader = styled.span(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.body};
    margin-left: 0.5em;
    color: ${theme.colors.gray[70]};
  `,
);

const ContentContainer = styled.div(
  ({ theme }) => css`
    border-left: 1px ${theme.colors.gray[90]} solid;
    margin-left: 7px;
    margin-top: 0px;
    padding-left: 25px;
  `,
);

const _filterInputProps = (props) => {
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { expanded, indetermined, stayExpanded, padded, ...inputProps } = props;

  return inputProps;
};

type Props = React.ComponentProps<typeof Checkbox> & {
  header: React.ReactNode;
  checked?: boolean;
  indetermined?: boolean;
  selectable?: boolean;
  expandable?: boolean;
  expanded?: boolean;
  stayExpanded?: boolean;
  subheader?: React.ReactNode;
  children?: React.ReactNode;
  padded?: boolean;
  readOnly?: boolean;
  onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void;
};

const ExpandableListItem = ({
  header,
  checked = false,
  indetermined = false,
  selectable = true,
  expandable = true,
  expanded = false,
  stayExpanded = false,
  subheader,
  children = [],
  padded = true,
  value,
  readOnly = false,
  onChange = () => undefined,
  ...otherProps
}: Props) => {
  const inputProps = _filterInputProps(otherProps);

  const _onChange = (e) => {
    e.stopPropagation();
    onChange(e);
  };

  return (
    <Accordion.Item value={value ?? String(header)}>
      <Accordion.Control>
        {selectable ? (
          <Checkbox
            title="Select item"
            checked={checked}
            readOnly={readOnly}
            onClick={(e) => e.stopPropagation()}
            onChange={_onChange}
            inline
            {...inputProps}>
            <span onClick={(e) => e.stopPropagation()}>{header}</span>
          </Checkbox>
        ) : (
          header
        )}

        {subheader && <Subheader>{subheader}</Subheader>}
      </Accordion.Control>
      <Accordion.Panel>
        <ContentContainer>{children}</ContentContainer>
      </Accordion.Panel>
    </Accordion.Item>
  );
};

export default ExpandableListItem;
