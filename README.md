# `lib-noir`

A set of utilities and helpers for building ring apps.

[![Continuous Integration status](https://api.travis-ci.org/noir-clojure/lib-noir.png)](http://travis-ci.org/noir-clojure/lib-noir)

Some of the core features includes the following:

* stateful sessions and cookies
* static resource management and file uploads
* custom response types and redirects
* input validation
* content caching
* route filtering and redirection
* password hashing using [jBcryt](http://www.mindrot.org/projects/jBCrypt/) 

See the [API](http://yogthos.github.com/lib-noir/index.html) for more details.

This library was originally split out from the [Noir](https://github.com/noir-clojure/noir) web framework
for your enjoyment.

This library is being actively developed separate from Noir. Nothing in here is specific to Noir and the purpose 
of it is to be used from any ring-based web framework, such as [Mustache](https://github.com/cgrand/moustache) 
and [Compojure](https://github.com/weavejester/compojure).

## Usage

You want to use [Leiningen](https://github.com/technomancy/leiningen), of course. Add this to your `:dependencies`

```clojure
[lib-noir "0.4.6"]
```

## Credits

A lot of these libraries were originally written by
[Chris Granger](https://github.com/ibdknox) and included in the
[Noir](https://github.com/noir-clojure/noir) web framework before its
deprecation. These libraries were split out into this library and additions and
changes have been made over time.

### Maintainers

* [Dmitri Sotnikov](https://github.com/yogthos)
* [Anthony Grimes](https://github.com/Raynes)

### Contributors

There is a very pretty list of contributors [here](https://github.com/noir-clojure/lib-noir/graphs/contributors)
