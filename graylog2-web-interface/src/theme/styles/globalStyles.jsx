import { css } from 'styled-components';

export const globalStyles = css(({ theme }) => css`
  body {
    background-color: ${theme.color.global.background};
    color: ${theme.color.global.textDefault};
    font-family: 'Open Sans', sans-serif;
    font-size: 14px;
    overflow-x: hidden;
    margin-top: 45px;
    min-height: calc(100vh - 45px);
  }

  ul {
    list-style-type: none;
    margin: 0;
  }

  ul.no-padding {
    padding: 0;
  }

  hr {
    border-top: 1px solid ${theme.color.global.background};
  }

  h1, h2, h3, h4, h5, h6 {
    font-weight: normal;
    padding: 0;
    margin: 0;
    color: ${theme.color.gray[10]}
  }

  h1 {
    font-size: 28px;
  }

  h2 {
    font-size: 21px;
  }

  h3 {
    font-size: 18px;
  }

  h4 {
    font-size: 14px;
    font-weight: bold;
  }

  h4 {
    font-size: 14px;
    font-weight: normal;
  }

  a {
    color: ${theme.color.global.link};

    :hover {
      color: ${theme.color.global.linkHover};
    }
  }

  /* Remove boostrap outline */
  a:active,
  select:active,
  input[type="file"]:active,
  input[type="radio"]:active,
  input[type="checkbox"]:active,
  .btn:active {
    outline: none;
    outline-offset: 0;
  }
`);

export default globalStyles;
