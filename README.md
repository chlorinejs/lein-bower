# lein-bower

Leiningen plugin for managing Bower dependencies in Clojure projects

## Installation

To enable lein-bower for your project, put the following in the
`:plugins` vector of your `project.clj` file:

![Latest version](https://clojars.org/lein-bower/latest-version.svg)

## Managing Bower dependencies

Like [NPM](https://github.com/bodil/lein-npm) dependencies,
bower components can be installed by adding a
`:bower-dependencies` key in your `project.clj`:

```clojure
  :bower-dependencies [[bootstrap "2.3.1"]
                       [font-awesome "3.0.2"]
                       [angular "~1.0.6"]
                       [angular-strap "~0.7.3"]
                       [angular-ui "~0.4.0"]
                       [angular-bootstrap "~0.3.0"]]
```

You can specify where bower components will be installed with:

```clojure
:bower {:directory "resources/public/js/lib"}
```

You can also specify what filenames to use for bower configuration:

```clojure
:bower {:package-file "bower.json", :config-file ".bowerrc"}
```

Users of bower versions prior to v0.9.0 may want to set `:package-file` to `"component.json"`. Default filenames are `bower.json` and `.bowerrc`.

## Invoking Bower

You can execute Bower commands that require the presence of two files
`bower.json` and `.bowerrc` using the `lein bower` command. This command
creates temporary versions of those files based on your `project.clj` before
invoking the Bower command you specify. The keys `name`, `description`, `version`
and `bower-dependencies` are automatically added to `bower.json`.
The key `bower-directory` is automatically added to `.bowerrc`.

```sh
$ lein bower install        # installs project dependencies
$ lein bower ls             # lists installed dependencies
$ lein bower search angular # searches for packages containing "angular"
```

## License

Copyright 2013 Hoang Minh Thang

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You may
obtain a copy of the License at
[http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0).

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License.
