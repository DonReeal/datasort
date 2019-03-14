# datasort - Exploring data visually by sorting

[Explore data](https://donreeal.github.io/datasort/) in a tabular UI using null friendly, deterministic sorting implemented in ClojureScript. The leading example for implementation is sorting json log messages with arbitrary properties. Inspired by the great *clojure.core/sort-by* function.

## datasort.criteriasort - DSL for multi-field comparators

### Limitations of the core apis that I know of

* Sorting based on *vectors* of sort partitions **does not allow for sorting nils last**
* Sorting nils last using *clojure.core/sort-by* requires a custom comparator that is **coupled to the structure of keyfn but cannot declared in-line**
* Sorting in reverse order using *clojure.core/sort-by* requires a custom comparator that is **coupled to the structure of keyfn but cannot declared in-line**

### API Requirements

Investigating on the core apis made me write down what I was looking for when declaring comparators for this project:

1. multi-field sorting must be supported (sorting in partitions)
1. each sort partition must be configurable to be used in natural or reversed order
1. it must be possible to sort nils last within each partition
1. dynamic generation of sort orders should be simple

I decided to build a custom dsl for declaring comparators - [example usage of datasort.criteriasort](src/cljs/datasort/criteriasort.md) .

## Setup


To get an interactive development environment run:

    lein figwheel

and open your browser at [localhost:3449](http://localhost:3449/).
This will auto compile and send all changes to the browser without the
need to reload. After the compilation process is complete, you will
get a Browser Connected REPL. An easy way to try it is:

    (js/alert "Am I connected?")

and you should see an alert in the browser window.

To clean all compiled files:

    lein clean

To create a production build run:

    lein do clean, cljsbuild once min

And open your browser in `resources/public/index.html`. You will not
get live reloading, nor a REPL.

## Interactive development with Cursive

To launch interactive development environment in cursive launch 
run configuration LOCAL REPL (defined in .idea/runConfigurations/LOCAL_REPL.xml)

## License

Distributed under the Eclipse Public License version 2.0
