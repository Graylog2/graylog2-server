import { createGlobalStyle } from 'styled-components';

const DisconnectedThemeStyles = createGlobalStyle`
  body {
    /* we love science */
    background: url('images/auth/loginbg.jpg') no-repeat center center fixed;
    background-size: cover;
  }

  #disconnected-box {
      margin-top: 120px;
  }

  #disconnected-box-content {
      background-color: #fff;
      -moz-box-shadow: 0 0 5px #888;
      -webkit-box-shadow: 0 0 5px#888;
      box-shadow: 0 0 40px #000;
  }

  .never-seen-warning {
      margin-top: 4px;
      margin-left: 10px;
  }

  .via-node-headline {
      margin-left: 20px;
  }

  .via-node-headline:hover {
      cursor: pointer;
  }

  .discovered-node-link {
      margin-left: 20px;
  }

  .discovered-node-link:hover {
      cursor: pointer;
  }

  #nodes-box {
      margin-top: 10px;
  }

  hr {
      margin-top: 10px;
      margin-bottom: 10px;
  }

  .loading-text {
      font-size: 14px;
  }

`;

export default DisconnectedThemeStyles;
