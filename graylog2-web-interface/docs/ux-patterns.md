### Form & Modal Submit Buttons

It is a challenge to unify the placement, stying and naming of form submit buttons across the application.
Please consider the following best practices when implementing or changing a form submit or cancel button:
- Rely on the shared components `FormSubmit` and `ModalSubmit` to implement the submit and cancel button.
  The `FormSubmit` can be used for all forms on pages. The `ModalSubmit` can be used for modals and similar element like 
  popovers or the login dialog.
- Make sure to follow the placements defined by these shared components. Currently:
  - submit buttons in form are vertically aligned with the inputs. The cancel button is being placed after the submit button.
  - submit buttons in modals are always placed in the right bottom corner. The cancel button is being placed before the submit button.
- When defining a name for the submit button
  - Instead of `Save` or `Ok` use a meaningful name for the submit button like `Create stream`.
  - Make sure to write only the first letter uppercase and all other letter lowercase. 
- Always use `Cancel` for the cancel button name.
