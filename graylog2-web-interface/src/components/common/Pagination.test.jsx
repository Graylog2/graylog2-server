import React from 'react';
import { fireEvent, render, cleanup } from 'wrappedTestingLibrary';

import Pagination from './Pagination';

describe('<Pagination />', () => {
  afterEach(cleanup);

  it('should display Pagination', () => {
    const currentPage = 1;
    const totalPages = 5;
    const { getByTestId } = render(<Pagination currentPage={currentPage}
                                               totalPages={totalPages} />);

    expect(getByTestId('graylog-pagination')).not.toBeNull();
    expect(getByTestId('graylog-pagination')).toMatchSnapshot();
  });

  it('should not render Pagination if only 1 page', () => {
    const currentPage = 1;
    const totalPages = 1;
    const { container } = render(<Pagination currentPage={currentPage}
                                             totalPages={totalPages} />);

    expect(container.firstChild).toBeNull();
  });

  it('should return proper page to `onChange`', () => {
    const currentPage = 2;
    const totalPages = 10;
    const onChangeSpy = jest.fn();
    const { getByLabelText } = render(<Pagination currentPage={currentPage}
                                                  totalPages={totalPages}
                                                  onChange={onChangeSpy} />);

    fireEvent.click(getByLabelText('Next'));

    expect(onChangeSpy).toHaveBeenLastCalledWith(currentPage + 1);
  });

  it('should return previous page to `onChange`', () => {
    const currentPage = 2;
    const totalPages = 10;
    const onChangeSpy = jest.fn();
    const { getByLabelText } = render(<Pagination currentPage={currentPage}
                                                  totalPages={totalPages}
                                                  onChange={onChangeSpy} />);

    fireEvent.click(getByLabelText('Prev'));

    expect(onChangeSpy).toHaveBeenLastCalledWith(currentPage - 1);
  });

  it('should return last page to `onChange`', () => {
    const currentPage = 2;
    const totalPages = 10;
    const onChangeSpy = jest.fn();
    const { getByLabelText } = render(<Pagination currentPage={currentPage}
                                                  totalPages={totalPages}
                                                  onChange={onChangeSpy} />);

    fireEvent.click(getByLabelText('Last'));

    expect(onChangeSpy).toHaveBeenLastCalledWith(totalPages);
  });

  it('should return first page to `onChange`', () => {
    const currentPage = 2;
    const totalPages = 10;
    const onChangeSpy = jest.fn();
    const { getByLabelText } = render(<Pagination currentPage={currentPage}
                                                  totalPages={totalPages}
                                                  onChange={onChangeSpy} />);

    fireEvent.click(getByLabelText('First'));

    expect(onChangeSpy).toHaveBeenLastCalledWith(1);
  });
});
