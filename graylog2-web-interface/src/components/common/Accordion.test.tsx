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
import * as React from 'react';
import { render, screen } from 'wrappedTestingLibrary';

import Accordion from './Accordion';
import AccordionItem from './AccordionItem';

describe('Accordion', () => {
  it('should render with all Items closed', () => {
    render(
      <Accordion id="accordion-test">
        <AccordionItem name="Example A">
          <h4>A as in Apple</h4>
        </AccordionItem>

        <AccordionItem name="Example 2">
          <h4>Two as in Teeth</h4>
        </AccordionItem>

        <AccordionItem name="Example III">
          <h4>Three as in Dimensions</h4>
        </AccordionItem>
      </Accordion>,
    );

    const expandedButtons = screen.queryAllByRole('button', { expanded: false });
    const tab1 = screen.getByLabelText('Example A');
    const tab2 = screen.getByLabelText('Example 2');
    const tab3 = screen.getByLabelText('Example III');

    expect(expandedButtons.length).toBe(3);
    expect(tab1).not.toHaveClass('in');
    expect(tab2).not.toHaveClass('in');
    expect(tab3).not.toHaveClass('in');
  });

  it('should render with one item opened', () => {
    render(
      <Accordion onSelect={jest.fn()}
                 id="accordion-test"
                 activeKey="example-a">
        <AccordionItem name="Example A">
          <h4>A as in Apple</h4>
        </AccordionItem>

        <AccordionItem name="Example 2">
          <h4>Two as in Teeth</h4>
        </AccordionItem>

        <AccordionItem name="Example III">
          <h4>Three as in Dimensions</h4>
        </AccordionItem>
      </Accordion>,
    );

    const expandedButtons = screen.queryAllByRole('button', { expanded: false });
    const tab1 = screen.getByLabelText('Example A');
    const tab2 = screen.getByLabelText('Example 2');
    const tab3 = screen.getByLabelText('Example III');

    expect(expandedButtons.length).toBe(2);
    expect(tab1).toHaveClass('in');
    expect(tab2).not.toHaveClass('in');
    expect(tab3).not.toHaveClass('in');
  });

  it('should render with activeKey as regular string', () => {
    render(
      <Accordion onSelect={jest.fn()}
                 id="accordion-test"
                 activeKey="Example 2">
        <AccordionItem name="Example A">
          <h4>A as in Apple</h4>
        </AccordionItem>

        <AccordionItem name="Example 2">
          <h4>Two as in Teeth</h4>
        </AccordionItem>

        <AccordionItem name="Example III">
          <h4>Three as in Dimensions</h4>
        </AccordionItem>
      </Accordion>,
    );

    const expandedButtons = screen.queryAllByRole('button', { expanded: false });
    const tab1 = screen.getByLabelText('Example A');
    const tab2 = screen.getByLabelText('Example 2');
    const tab3 = screen.getByLabelText('Example III');

    expect(expandedButtons.length).toBe(2);
    expect(tab1).not.toHaveClass('in');
    expect(tab2).toHaveClass('in');
    expect(tab3).not.toHaveClass('in');
  });
});
