# To prepare accesible drag and drop implementation all current interactions must be supported by keyboard input

## Requirements

## Research

* [Accessible Table Navigation](https://www.w3.org/TR/wai-aria-practices/examples/table/table.html) 

* [Accessible Drag and Drop Example](https://www.w3.org/blog/wai-components-gallery/widget/accessible-drag-and-drop/)

* [google fundamentals accessibility](https://developers.google.com/web/fundamentals/accessibility/focus/using-tabindex)

* Tabbable html elements [tabindex](https://www.w3.org/TR/html5/editing.html#the-tabindex-attribute)
  * Persist navigation state within navigable elements [example](https://www.w3.org/TR/wai-aria-practices/examples/grid/dataGrids.html)
    1. Tab -> activeElement is first element of table
    1. Right-Arrow -> activeElement ist second header
    1. Tab -> activeElement is next tabbable element
    1. Shift + Tab -> activeElement ist second header of table

