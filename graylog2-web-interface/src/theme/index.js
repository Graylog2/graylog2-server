import teinte from './teinte';
import breakpoints from './breakpoints';

const theme = {
  teinte: {
    ...teinte,
  },
  breakpoint: {
    ...breakpoints,
  },
};

const themeModes = ['teinte', 'noire'];

export default theme;

export {
  breakpoints as breakpoint,
  teinte,
  themeModes,
};
