# funes

Infer the general shape of data from at least two examples, and optionally
construct a Schema from it.

[![Travis Badge](https://img.shields.io/travis/txus/funes/master.svg)](https://travis-ci.org/txus/funes "Travis Badge")

[![Clojars Project](http://clojars.org/funes/latest-version.svg)](http://clojars.org/funes)

```
I suspect, nevertheless, that he was not very capable of thought. To think is to
forget a difference, to generalize, to abstract. In the overly replete world of
Funes there were nothing but details, almost contiguous details.

-- from "Funes the Memorious", by Jorge Luis Borges
```

## Usage

```clojure
(require '[funes.core :as f])

(let [data [{:age 20 :name "Funes" :traits [:memorious]}
            {:age 40 :name "Borges" :traits [:writer]}
            {:name "Dog" :breed "doge"}]]
  (f/->schema data))

; {:name                              schema.core/Str
;  (schema.core/optional-key :age)    schema.core/Num
;  (schema.core/optional-key :traits) [(schema.core/enum :writer :memorious)]
;  (schema.core/optional-key :breed)  schema.core/Str}
```

By default, Funes tries to achieve a good tradeoff between generalization and
concreteness. If you need deeper control into that, you can use the pieces
yourself. The above call to `(f/->schema data)` evaluates to:

```clojure
(require '[funes.schema :as s])

(->> data
     (reduce f/infer)
     s/generalize-values ;; you can skip this part if you want maximum concreteness
     s/generate-schema)
```

## What about clojure.spec?

Funes is in no way coupled to `prismatic/schema`. The namespace that generates
schemas is `funes.schema`, and it's just 100 lines of code describing a
compilear from Funes' internal AST to Schema, but it should be equally trivial
to compile to `clojure.spec`. I'd like to to that soon, but feel free to give it
a go if you'd like!

## License

The MIT License (MIT)

Copyright © 2017 Txus

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
