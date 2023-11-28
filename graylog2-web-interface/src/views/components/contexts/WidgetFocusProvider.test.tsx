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
import { render, screen, fireEvent } from 'wrappedTestingLibrary';

import useLocation from 'routing/useLocation';
import { asMock } from 'helpers/mocking';
import WidgetFocusProvider from 'views/components/contexts/WidgetFocusProvider';
import type { WidgetFocusContextType } from 'views/components/contexts/WidgetFocusContext';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';
import { loadViewsPlugin, unloadViewsPlugin } from 'views/test/testViewsPlugin';
import TestStoreProvider from 'views/test/TestStoreProvider';
import { allMessagesTable } from 'views/logic/Widgets';
import { createViewWithWidgets } from 'fixtures/searches';
import useAppDispatch from 'stores/useAppDispatch';
import { Button } from 'components/bootstrap';

const mockNavigate = jest.fn();

jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
  useLocation: jest.fn(() => ({
    pathname: '',
    search: '',
  })),
}));

jest.mock('stores/useAppDispatch');

const emptyLocation = {
  pathname: '',
  search: '',
  hash: '',
  state: undefined,
  key: '',
};

const ShowFocusedWidget = ({ focusedWidget }: WidgetFocusContextType) => (focusedWidget ? (
  <span>Focused widget: {JSON.stringify(focusedWidget)}</span>
) : (
  <span>No focused widget</span>
));

jest.mock('views/logic/slices/searchExecutionSlice', () => ({
  ...jest.requireActual('views/logic/slices/searchExecutionSlice'),
  execute: jest.fn(() => async () => {}),
}));

describe('WidgetFocusProvider', () => {
  beforeAll(loadViewsPlugin);

  afterAll(unloadViewsPlugin);

  beforeEach(() => {
    const dispatch = jest.fn();
    asMock(useAppDispatch).mockReturnValue(dispatch);
    asMock(useLocation).mockReturnValue(emptyLocation);
  });

  const renderSUT = (consume: (value: WidgetFocusContextType) => JSX.Element) => {
    const widget = allMessagesTable('widget-id');
    const view = createViewWithWidgets([widget], {});

    return render(
      <TestStoreProvider view={view}>
        <WidgetFocusProvider>
          <WidgetFocusContext.Consumer>
            {consume}
          </WidgetFocusContext.Consumer>
        </WidgetFocusProvider>
      </TestStoreProvider>,
    );
  };

  it('should update url on widget focus', async () => {
    const consume = ({ setWidgetFocusing }: WidgetFocusContextType) => (
      <Button onClick={() => setWidgetFocusing('widget-id')}>Focus!</Button>
    );

    renderSUT(consume);

    const button = await screen.findByRole('button', { name: 'Focus!' });

    fireEvent.click(button);

    expect(mockNavigate).toHaveBeenCalledWith('?focusedId=widget-id&focusing=true', { replace: true });
  });

  it('should update url on widget focus close', async () => {
    asMock(useLocation).mockReturnValueOnce({
      ...emptyLocation,
      search: '?focusedId=widget-id&focusing=true',
    });

    const consume = ({ unsetWidgetFocusing }: WidgetFocusContextType) => (
      <Button onClick={() => unsetWidgetFocusing()}>Unfocus!</Button>
    );

    renderSUT(consume);

    const button = await screen.findByRole('button', { name: 'Unfocus!' });
    fireEvent.click(button);

    expect(mockNavigate).toHaveBeenLastCalledWith('', { replace: true });
  });

  it('should set widget focus based on url', async () => {
    asMock(useLocation).mockReturnValue({
      ...emptyLocation,
      search: '?focusedId=widget-id&focusing=true',
    });

    renderSUT(ShowFocusedWidget);

    await screen.findByText(/Focused widget: {"id":"widget-id","editing":false,"focusing":true}/);
  });

  it('should update url on widget edit', async () => {
    const consume = ({ setWidgetEditing }: WidgetFocusContextType) => (
      <Button onClick={() => setWidgetEditing('widget-id')}>Edit!</Button>
    );

    renderSUT(consume);

    const button = await screen.findByRole('button', { name: 'Edit!' });
    fireEvent.click(button);

    expect(mockNavigate).toHaveBeenCalledWith('?focusedId=widget-id&editing=true', { replace: true });
  });

  it('should update url on widget edit close', async () => {
    asMock(useLocation).mockReturnValue({
      ...emptyLocation,
      search: '?focusedId=widget-id&editing=true',
    });

    const consume = ({ unsetWidgetEditing }: WidgetFocusContextType) => (
      <Button onClick={() => unsetWidgetEditing()}>Cancel Edit!</Button>
    );

    renderSUT(consume);

    const button = await screen.findByRole('button', { name: 'Cancel Edit!' });
    fireEvent.click(button);

    expect(mockNavigate).toHaveBeenCalledWith('', { replace: true });
  });

  it('should set widget edit and focused based on url', async () => {
    asMock(useLocation).mockReturnValue({
      ...emptyLocation,
      search: '?focusedId=widget-id&editing=true',
    });

    renderSUT(ShowFocusedWidget);

    await screen.findByText(/Focused widget: {"id":"widget-id","editing":true,"focusing":true}/);
  });

  it('should not remove focus query param on widget edit', async () => {
    asMock(useLocation).mockReturnValue({
      ...emptyLocation,
      search: '?focusedId=widget-id&focusing=true',
    });

    const consume = ({ setWidgetEditing, unsetWidgetEditing }: WidgetFocusContextType) => (
      <>
        <Button onClick={() => setWidgetEditing('widget-id')}>Edit</Button>
        <Button onClick={() => unsetWidgetEditing()}>Cancel</Button>
      </>
    );

    renderSUT(consume);

    fireEvent.click(await screen.findByRole('button', { name: 'Edit' }));

    expect(mockNavigate).toHaveBeenCalledWith('?focusedId=widget-id&focusing=true&editing=true', { replace: true });

    fireEvent.click(await screen.findByRole('button', { name: 'Cancel' }));

    expect(mockNavigate).toHaveBeenCalledWith('?focusedId=widget-id&focusing=true', { replace: true });
  });

  it('should not set focused widget from url and cleanup url if the widget does not exist', async () => {
    asMock(useLocation).mockReturnValue({
      ...emptyLocation,
      search: '?focusedId=not-existing-widget-id',
    });

    renderSUT(ShowFocusedWidget);

    await screen.findByText(/No focused widget/);

    expect(mockNavigate).toHaveBeenLastCalledWith('', { replace: true });
  });

  it('does not trigger setting widgets to search initially', () => {
    const dispatch = jest.fn();
    asMock(useAppDispatch).mockReturnValue(dispatch);
    asMock(useLocation).mockReturnValue(emptyLocation);
    renderSUT(jest.fn());

    expect(dispatch).not.toHaveBeenCalled();
  });
});
