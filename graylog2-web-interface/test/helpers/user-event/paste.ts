import userEvent from '@testing-library/user-event';

const paste = (elem: HTMLElement, text: string) => userEvent.paste(elem, text);

export default paste;
