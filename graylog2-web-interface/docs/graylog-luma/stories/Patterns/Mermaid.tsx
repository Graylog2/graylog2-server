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
import React, { useEffect, useRef } from 'react';
import mermaid from 'mermaid';
import styled, { useTheme } from 'styled-components';
import { Unstyled } from '@storybook/addon-docs/blocks';

let _counter = 0;

type Props = { chart: string };

const Container = styled.div(
  ({ theme }) => `
    display: flex;
    justify-content: center;
    margin-bottom: ${theme.spacings.lg};

    .label {
      font-weight: normal;
    }

    .cluster rect {
      stroke-dasharray: 6, 4;
    }
  `,
);

const Mermaid = ({ chart }: Props) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const idRef = useRef(`mermaid-${++_counter}`);
  const theme = useTheme();

  useEffect(() => {
    if (!containerRef.current) return;

    mermaid.initialize({
      startOnLoad: false,
      theme: 'base',
      fontFamily: 'inherit',
      themeVariables: {
        primaryColor: theme.colors.global.contentBackground,
        primaryBorderColor: theme.colors.input.border,
        primaryTextColor: theme.colors.text.primary,
        lineColor: theme.colors.text.secondary,
        edgeLabelBackground: theme.colors.global.contentBackground,
        clusterBkg: 'transparent',
        clusterBorder: theme.colors.input.border,
        titleColor: theme.colors.text.primary,
      },
    });

    mermaid.render(idRef.current, chart).then(({ svg }) => {
      if (containerRef.current) containerRef.current.innerHTML = svg;
    });
  }, [chart, theme]);

  return (
    <Unstyled>
      <Container ref={containerRef} />
    </Unstyled>
  );
};

export default Mermaid;
