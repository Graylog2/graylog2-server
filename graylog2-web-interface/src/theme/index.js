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

export default theme;

export {
  breakpoints as breakpoint,
  teinte,
};
