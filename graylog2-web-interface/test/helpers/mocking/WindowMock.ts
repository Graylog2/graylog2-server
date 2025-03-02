// eslint-disable-next-line import/prefer-default-export
export const useWindowConfirmMock = () => {
  let originalWindowConfirm;

  beforeEach(() => {
    originalWindowConfirm = window.confirm;
    window.confirm = jest.fn(() => true);
  });

  afterEach(() => {
    window.confirm = originalWindowConfirm;
  });
};
