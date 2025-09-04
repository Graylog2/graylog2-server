import * as React from 'react';
import styled, { css } from 'styled-components';
import { Accordion } from '@mantine/core';

const ContentContainer = styled.div(
  ({ theme }) => css`
    border-left: 1px ${theme.colors.gray[90]} solid;
    padding-left: 18px;
  `,
);

const Subheader = styled.span(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.body};
    margin-left: 0.5em;
    color: ${theme.colors.gray[70]};
  `,
);

type Props = {
  header: React.ReactNode;
  value: string;
  children: React.ReactNode;
  subheader?: React.ReactNode;
};

const ExpandableListItem = ({ header, children, value, subheader = undefined }: Props) => (
  <Accordion.Item value={value}>
    <Accordion.Control>
      {header}
      {subheader && <Subheader>{subheader}</Subheader>}
    </Accordion.Control>
    <Accordion.Panel>
      <ContentContainer>{children}</ContentContainer>
    </Accordion.Panel>
  </Accordion.Item>
);

export default ExpandableListItem;
