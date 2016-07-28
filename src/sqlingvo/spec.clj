(ns sqlingvo.spec
  (:require [clojure.spec :as s]
            [clojure.spec.test :refer [instrument]]
            [sqlingvo.core :as core]
            [sqlingvo.expr :as expr]
            [sqlingvo.util :as util]))

(defn column?
  "Returns true if `x` is a column, otherwise false."
  [x]
  (and (map? x) (= (:op x) :column)))

(defn table?
  "Returns true if `x` is a table, otherwise false."
  [x]
  (and (map? x) (= (:op x) :table)))

(s/def ::identifier
  (s/or :keyword keyword?
        :string string?
        :symbol symbol?))

(s/def ::op
  #{:alias
    :array
    :attr
    :check
    :column
    :condition
    :constant
    :copy
    :create-table
    :delete
    :distinct
    :drop-materialized-view
    :drop-table
    :explain
    :expr-list
    :insert
    :join
    :like
    :list
    :nil
    :on-conflict
    :on-conflict-on-constraint
    :refresh-materialized-view
    :select
    :table
    :truncate
    :update
    :values
    :with})

(s/def ::name keyword?)
(s/def ::table keyword?)
(s/def ::schema keyword?)
(s/def ::node (s/keys :req-un [::op]))

(s/def ::alias
  (s/keys :req-un [::op ::name]))

(s/def ::column-node
  (s/keys :req-un [::op ::name]
          :opt-un [::schema ::table]))

(s/def ::table-node
  (s/keys :req-un [::op ::name]
          :opt-un [::schema ::table]))

(def column-identifier
  "The column identifier."
  (s/or :alias ::alias
        :column column?
        :identifier ::identifier))

(def table-identifier
  "The table identifier."
  (s/or :alias ::alias
        :identifier ::identifier
        :table table?))

(s/fdef expr/parse-expr
  :args (s/cat :expr any?)
  :ret ::node)

(s/fdef expr/parse-column
  :args (s/cat :s column-identifier)
  :ret ::column-node)

(s/fdef expr/parse-table
  :args (s/cat :s table-identifier)
  :ret ::table-node)

(s/fdef util/sql-keyword-hyphenate
  :args (s/cat :identifier (s/nilable ::identifier))
  :ret (s/nilable string?))

(s/fdef util/sql-name-underscore
  :args (s/cat :identifier (s/nilable ::identifier))
  :ret (s/nilable string?))

(s/fdef util/sql-type-name
  :args (s/cat :identifier (s/nilable ::identifier))
  :ret (s/nilable string?))
