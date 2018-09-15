# `lib-noir`

## this project is deprecated and it's no longer actively maintained 

A set of utilities and helpers for building ring apps.

[![Continuous Integration status](https://api.travis-ci.org/noir-clojure/lib-noir.png)](http://travis-ci.org/noir-clojure/lib-noir)

Some of the core features includes the following:

* stateful sessions and cookies
* static resource management and file uploads
* custom response types and redirects
* input validation
* content caching
* route filtering and redirection
* password hashing using [SCrypt](https://github.com/clojurewerkz/scrypt)

See the [API](http://yogthos.github.com/lib-noir/index.html) for more details.

This library was originally split out from the [Noir](https://github.com/noir-clojure/noir) web framework
for your enjoyment.

This library is being actively developed separate from Noir. Nothing in here is specific to Noir and the purpose
of it is to be used from any ring-based web framework, such as [Moustache](https://github.com/cgrand/moustache)
and [Compojure](https://github.com/weavejester/compojure).

## Usage

You want to use [Leiningen](https://github.com/technomancy/leiningen), of course. Add this to your `:dependencies`

[![Clojars Project](http://clojars.org/lib-noir/latest-version.svg)](http://clojars.org/lib-noir)

## Breaking changes in 0.9.5

lib-noir now uses [scrypt](https://github.com/clojurewerkz/scrypt) as its crypto implementation. This breaks compatibility with the existing password hashes encrypted using bcrypt.

## Breaking changes in 0.7.4

The `:store` key in `noir.util.middleware/app-handler` has been replaced with the `:session-options` key that allows specifying any Ring session parameters, eg:

```clojure
(def app
  (middleware/app-handler
    [home-routes app-routes]
    :session-options {:cookie-name "example-app-session"
                      :store (cookie-store)}))
```

## Breaking changes in 0.6.2

The access rule handling has been changed in 0.6.2, please see [documentation](http://www.luminusweb.net/docs/routes.md#restricting_access) for details

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
