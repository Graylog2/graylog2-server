declare module '*.css' {
  interface CSSClasses { [key: string]: any }
  const classes: CSSClasses;
  export default classes;
}
