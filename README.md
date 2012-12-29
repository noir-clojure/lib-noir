# lib-noir

A set of libraries for ring apps, including stateful sessions.

All of these libraries were split out from the [Noir](https://github.com/noir-clojure/noir) web framework
for your enjoyment. There are libraries/middleware for stateful sessions, validation, cookies, as well as
utilities for creating ring response maps (higher level than `ring.util.response`) and encrypting passwords
and such.

These libraries will be maintained separate from Noir, and if you already use the Noir web framework, you
already have this library. Nothing in here is specific to Noir and the purpose of it is to be used from
any ring-based web framework, such as mustache and compojure.

## Usage

You want to use leiningen, of course. Add this to your `:dependencies`

```clojure
[lib-noir "0.3.0"]
```
