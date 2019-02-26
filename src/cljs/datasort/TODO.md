# TODOS


## Sorting

* [x] API based on passing functions (DONE: criteriasort2)
* [x] API for declaring comparators on single properties (DONE: criteriasort2)
* [x] Performance tuning on sorting 
      
      * consider https://en.wikipedia.org/wiki/Schwartzian_transform (decorate-sort-undecorate) for speedup
      * consider progress indicator on sorting to smoothen ux
      * consider sorting async https://stackoverflow.com/questions/45661247/implement-async-await-in-sort-function-of-arrays-javascript
      * consider using nil-checking instead of nesting function calls for low level comparators in datasort.criteriasort/cmp, datasort.criteriasort/cmp-fn

## Table-UI

* [x] render table fn based on data (DONE: core/render-table)
* [x] find a table ui-model with minimal state to allow for declaring sorting by multiple columns

        * Sorting by a column should put nils last in natural order
        * For each column a user must be able to toggle natural and reverse order
        * Sorting by multiple columns should be configurable by interacting with the table itself (no config dialogue)
* [x] Build initial state with minimal user input (ident-fn, records)
* [x] Integrate UI-state to render on toggle column order            
* [ ] build [<|******|>] shift ui-component  (wrapper-component)
* [x] integrate shift with reordering columns, update sort order accordingly
* [ ] build [:asc|:desc] toggle ui-component (leaf-component) [toggle component talk](https://youtu.be/b_uum_iYShE?t=486)           


## file loading

see:
* https://gist.github.com/paultopia/6fc396884c223b619f2e2ef199866fdd
* https://mrmcc3.github.io/blog/posts/csv-with-clojurescript/
* https://scotch.io/tutorials/use-the-html5-file-api-to-work-with-files-locally-in-the-browser

  
