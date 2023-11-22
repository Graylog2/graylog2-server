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

import type { Steps } from 'components/common/Wizard';
import Wizard from 'components/common/Wizard';
import asMock from 'helpers/mocking/AsMock';

const previousButton = () => screen.findByRole('button', { name: /Previous/i });
const nextButton = () => screen.findByRole('button', { name: /Next/i });

describe('<Wizard />', () => {
  const steps: Steps = [
    { key: 'Key1', title: 'Title1', component: (<div>Component1</div>) },
    { key: 'Key2', title: 'Title2', component: (<div>Component2</div>) },
    { key: 'Key3', title: 'Title3', component: (<div>Component3</div>) },
  ];

  it('should render with 3 steps', async () => {
    render(<Wizard steps={steps} />);

    await screen.findByText('Component1');
  });

  it('should render with 3 steps and children', async () => {
    render(<Wizard steps={steps}><span>Preview</span></Wizard>);

    await screen.findByText('Preview');
  });

  it('should render in horizontal mode with 3 steps', async () => {
    render(<Wizard steps={steps} horizontal />);

    await screen.findByText('Component1');
  });

  it('should render in horizontal mode with 3 steps and children', async () => {
    render(<Wizard steps={steps} horizontal><span>Preview</span></Wizard>);

    await screen.findByText('Preview');
  });

  describe('When used in an uncontrolled way', () => {
    it('should render step 1 when nothing was clicked', async () => {
      render(<Wizard steps={steps} />);

      await screen.findByText('Component1');

      expect(screen.queryByText('Component2')).not.toBeInTheDocument();
      expect(screen.queryByText('Component3')).not.toBeInTheDocument();

      expect(await previousButton()).toBeDisabled();
      expect(await nextButton()).not.toBeDisabled();
    });

    it('should render step 2 when clicked on step 2', async () => {
      render(<Wizard steps={steps} />);

      await screen.findByText('Title1');
      screen.getByText('Title2').click();

      await screen.findByText('Component2');

      expect(screen.queryByText('Component1')).not.toBeInTheDocument();
      expect(screen.queryByText('Component3')).not.toBeInTheDocument();

      expect(await previousButton()).not.toBeDisabled();
      expect(await nextButton()).not.toBeDisabled();
    });

    it('should render step 2 when clicked on next', async () => {
      render(<Wizard steps={steps} />);

      (await nextButton()).click();

      await screen.findByText('Component2');

      expect(screen.queryByText('Component1')).not.toBeInTheDocument();
      expect(screen.queryByText('Component3')).not.toBeInTheDocument();

      expect(await previousButton()).not.toBeDisabled();
      expect(await nextButton()).not.toBeDisabled();
    });

    it('should render step 3 when two times clicked on next', async () => {
      render(<Wizard steps={steps} />);

      (await nextButton()).click();
      await screen.findByText('Component2');
      (await nextButton()).click();

      await screen.findByText('Component3');

      expect(screen.queryByText('Component1')).not.toBeInTheDocument();
      expect(screen.queryByText('Component2')).not.toBeInTheDocument();

      expect(await previousButton()).not.toBeDisabled();
      expect(await nextButton()).toBeDisabled();
    });

    it('should render step 2 when two times clicked on next and one time clicked on previous', async () => {
      const changeFn = jest.fn(() => {});
      render(<Wizard steps={steps} onStepChange={changeFn} />);

      (await nextButton()).click();
      await screen.findByText('Component2');
      (await nextButton()).click();
      await screen.findByText('Component3');
      (await previousButton()).click();

      await screen.findByText('Component2');

      expect(screen.queryByText('Component1')).not.toBeInTheDocument();
      expect(screen.queryByText('Component3')).not.toBeInTheDocument();

      expect(await previousButton()).not.toBeDisabled();
      expect(await nextButton()).not.toBeDisabled();
      expect(changeFn).toHaveBeenCalledTimes(3);
    });
  });

  describe('When used in a controlled way', () => {
    it('should render active step given from prop', async () => {
      render(<Wizard steps={steps} activeStep="Key2" />);

      await screen.findByText('Component2');

      expect(screen.queryByText('Component1')).not.toBeInTheDocument();
      expect(screen.queryByText('Component3')).not.toBeInTheDocument();

      (await nextButton()).click();

      await screen.findByText('Component2');

      expect(screen.queryByText('Component1')).not.toBeInTheDocument();
      expect(screen.queryByText('Component3')).not.toBeInTheDocument();
    });

    it('should change the active step when prop changes', async () => {
      const { rerender } = render(<Wizard steps={steps} activeStep="Key2" />);

      await screen.findByText('Component2');

      expect(screen.queryByText('Component1')).not.toBeInTheDocument();
      expect(screen.queryByText('Component3')).not.toBeInTheDocument();

      rerender(<Wizard steps={steps} activeStep="Key1" />);

      await screen.findByText('Component1');

      expect(screen.queryByText('Component2')).not.toBeInTheDocument();
      expect(screen.queryByText('Component3')).not.toBeInTheDocument();
    });

    it('should show a warning when activeStep is not a key in steps', async () => {
      /* eslint-disable no-console */
      const consoleWarn = console.warn;

      console.warn = jest.fn();
      const { rerender } = render(<Wizard steps={steps} activeStep={0} />);

      await screen.findByText('Component1');

      expect(screen.queryByText('Component2')).not.toBeInTheDocument();
      expect(screen.queryByText('Component3')).not.toBeInTheDocument();
      expect(console.warn).toHaveBeenCalledTimes(1);

      rerender(<Wizard steps={steps} activeStep="Key12314" />);

      await screen.findByText('Component1');

      expect(screen.queryByText('Component2')).not.toBeInTheDocument();
      expect(screen.queryByText('Component3')).not.toBeInTheDocument();
      expect(console.warn).toHaveBeenCalledTimes(2);

      console.warn = consoleWarn;
      /* eslint-enable no-console */
    });
  });

  it('should give callback step when changing the step', async () => {
    const changeFn = jest.fn((step) => {
      expect(step).toEqual('Key2');
    });

    const { rerender } = render(<Wizard steps={steps} onStepChange={changeFn} />);

    (await nextButton()).click();

    expect(changeFn).toHaveBeenCalled();

    rerender(<Wizard steps={steps} onStepChange={changeFn} activeStep="Key1" />);

    asMock(changeFn).mockClear();
    (await nextButton()).click();

    expect(changeFn).toHaveBeenCalled();
  });

  it('should respect disabled flag for a step', async () => {
    steps[1].disabled = true;
    steps[2].disabled = true;
    render(<Wizard steps={steps} />);

    (await nextButton()).click();

    await screen.findByText('Component1');

    expect(screen.queryByText('Component2')).not.toBeInTheDocument();
    expect(screen.queryByText('Component3')).not.toBeInTheDocument();

    (await screen.findByText('Title2')).click();

    await screen.findByText('Component1');

    expect(screen.queryByText('Component2')).not.toBeInTheDocument();
    expect(screen.queryByText('Component3')).not.toBeInTheDocument();
  });

  it('should render next/previous buttons by default', async () => {
    const { rerender } = render(<Wizard steps={steps} />);

    expect(await nextButton()).toBeInTheDocument();
    expect(await previousButton()).toBeInTheDocument();

    rerender(<Wizard steps={steps} horizontal />);

    expect(screen.getByLabelText('Next')).toBeInTheDocument();
    expect(screen.getByLabelText('Previous')).toBeInTheDocument();
  });

  it('should hide next/previous buttons if hidePreviousNextButtons is set', async () => {
    const { rerender } = render(<Wizard steps={steps} hidePreviousNextButtons />);

    expect(screen.queryByRole('button', { name: 'Next' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Previous' })).not.toBeInTheDocument();

    rerender(<Wizard steps={steps} horizontal hidePreviousNextButtons />);

    expect(screen.queryByLabelText('Next')).not.toBeInTheDocument();
    expect(screen.queryByLabelText('Previous')).not.toBeInTheDocument();
  });
});
