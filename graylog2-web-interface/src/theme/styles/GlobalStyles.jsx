import { createGlobalStyle } from 'styled-components';
import { color } from 'theme';

const GlobalStyles = createGlobalStyle`
  body {
    background-color: ${color.global.background};
    color: ${color.global.textDefault};
    font-family: 'Open Sans', sans-serif;
    font-size: 12px;
    overflow-x: hidden;
    margin-top: 45px;
  }

  ul {
    list-style-type: none;
    margin: 0;
  }

  ul.no-padding {
    padding: 0;
  }

  hr {
    border-top: 1px solid ${color.global.background};
  }

  h1, h2, h3, h4, h5, h6 {
    font-weight: normal;
    padding: 0;
    margin: 0;
    color: ${color.gray[10]}
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
    color: ${color.global.link};

    :hover {
      color: ${color.global.linkHover};
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
`;

export default GlobalStyles;
