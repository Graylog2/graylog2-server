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
import React from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import copyToClipboard from 'util/copyToClipboard';

import JSONClipboardIconButton from './JSONClipboardIconButton';

jest.mock('util/copyToClipboard', () => jest.fn(() => Promise.resolve()));

describe('JSONClipboardIconButton', () => {
  it('should copy JSON-serialized object to clipboard', async () => {
    const content = { key: 'value', nested: { num: 42 } };
    render(<JSONClipboardIconButton content={content} buttonTitle="Copy JSON" />);

    await userEvent.click(await screen.findByRole('button', { name: /copy json/i }));

    expect(copyToClipboard).toHaveBeenCalledWith(JSON.stringify(content, null, 2));

    await screen.findByText('Copied!');
  });

  it('should serialize a BigInt', async () => {
    const content = BigInt('9007199254740993');
    render(<JSONClipboardIconButton content={content} buttonTitle="Copy BigInt" />);

    await userEvent.click(await screen.findByRole('button', { name: /copy bigint/i }));

    expect(copyToClipboard).toHaveBeenCalledWith('9007199254740993');

    await screen.findByText('Copied!');
  });

  it('should serialize an object containing a BigInt', async () => {
    const content = { id: BigInt('9007199254740993'), name: 'test' };
    render(<JSONClipboardIconButton content={content} buttonTitle="Copy object" />);

    await userEvent.click(await screen.findByRole('button', { name: /copy object/i }));

    expect(copyToClipboard).toHaveBeenCalledWith('{\n  "id": 9007199254740993,\n  "name": "test"\n}');

    await screen.findByText('Copied!');
  });

  it('should serialize an array containing a BigInt', async () => {
    const content = [BigInt('9007199254740993'), 'text', 42];
    render(<JSONClipboardIconButton content={content} buttonTitle="Copy array" />);

    await userEvent.click(await screen.findByRole('button', { name: /copy array/i }));

    expect(copyToClipboard).toHaveBeenCalledWith('[\n  9007199254740993,\n  "text",\n  42\n]');

    await screen.findByText('Copied!');
  });
});
