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

import Sanitize from './Sanitize';

const TEST_ID = 'sanitize-target';

describe('Sanitize', () => {
  it('renders safe HTML inside a span', () => {
    render(<Sanitize html="<b>bold</b> and <i>italic</i>" data-testid={TEST_ID} />);

    const span = screen.getByTestId(TEST_ID);

    expect(span.innerHTML).toBe('<b>bold</b> and <i>italic</i>');
  });

  it('strips <script> tags', () => {
    render(<Sanitize html={'hello<script>alert(1)</script>world'} data-testid={TEST_ID} />);

    const span = screen.getByTestId(TEST_ID);

    expect(span.innerHTML).not.toContain('<script');
    expect(span.textContent).toBe('helloworld');
  });

  it('strips inline event handlers', () => {
    render(<Sanitize html={'<img src="x" onerror="alert(1)" alt="broken" />'} data-testid={TEST_ID} />);

    const img = screen.getByAltText('broken');

    expect(img.getAttribute('onerror')).toBeNull();
  });

  it.each([
    ['undefined', undefined],
    ['null', null],
    ['empty string', ''],
  ])('renders an empty span for %s', (_label, input) => {
    render(<Sanitize html={input} data-testid={TEST_ID} />);

    const span = screen.getByTestId(TEST_ID);

    expect(span.innerHTML).toBe('');
  });

  it('passes HTML attributes through to the span', () => {
    render(<Sanitize html="hello" className="my-class" title="tooltip" data-testid={TEST_ID} />);

    const span = screen.getByTestId(TEST_ID);

    expect(span.getAttribute('class')).toBe('my-class');
    expect(span.getAttribute('title')).toBe('tooltip');
    expect(span.tagName).toBe('SPAN');
  });

  it('honors a custom config that strips all tags', () => {
    render(<Sanitize html="<b>keep text</b>" config={{ ALLOWED_TAGS: [] }} data-testid={TEST_ID} />);

    const span = screen.getByTestId(TEST_ID);

    expect(span.innerHTML).not.toContain('<b');
    expect(span.textContent).toBe('keep text');
  });
});
