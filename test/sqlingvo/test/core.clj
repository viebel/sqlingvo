(ns sqlingvo.test.core
  (:refer-clojure :exclude [distinct group-by replace])
  (:require [clojure.java.jdbc :as jdbc])
  (:use clojure.test
        sqlingvo.core))

(deftest test-insert
  (are [stmt expected]
       (is (= expected (sql stmt)))
       (-> (insert :films
               (-> (select *)
                   (from :tmp-films)
                   (where '(< :date-prod "2004-05-07")))))
       ["INSERT INTO films SELECT * FROM tmp-films WHERE (date-prod < ?)" "2004-05-07"]
       (insert :airports [:country-id, :name :gps-code :iata-code :wikipedia-url :location]
         (-> (select (distinct [:c.id :a.name :a.gps-code :a.iata-code :a.wikipedia :a.geom] :on [:a.iata-code]))
             (from (as :natural-earth.airports :a))
             (join (as :countries :c) '(on (:&& :c.geography :a.geom)))
             (join :airports '(on (= :airports.iata-code :a.iata-code)) :type :left)
             (where '(and (is-not-null :a.gps-code)
                          (is-not-null :a.iata-code)
                          (is-null :airports.iata-code)))))
       [(str "INSERT INTO airports (country-id, name, gps-code, iata-code, wikipedia-url, location) "
             "SELECT DISTINCT ON (a.iata-code) c.id, a.name, a.gps-code, a.iata-code, a.wikipedia, a.geom "
             "FROM natural-earth.airports AS a "
             "LEFT JOIN airports ON (airports.iata-code = a.iata-code) "
             "JOIN countries AS c ON (c.geography && a.geom) "
             "WHERE ((a.gps-code IS NOT NULL) and (a.iata-code IS NOT NULL) and (airports.iata-code IS NULL))")]))

(deftest test-update
  (are [stmt expected]
       (is (= expected (sql stmt)))
       (-> (update :quotes '((= :daily-return :u.daily-return)))
           (where '(= :quotes.id :u.id))
           (from (as (-> (select :id (as '((lag :close) over (partition by :company-id order by :date desc)) :daily-return))
                         (from :quotes)) :u)))
       ["UPDATE quotes SET daily-return = u.daily-return FROM (SELECT id, lag(close) over (partition by company-id order by date desc) AS daily-return FROM quotes) AS u WHERE (quotes.id = u.id)"]
       (let [quote {:id 1}]
         (-> (update :prices '((= :daily-return :u.daily-return)))
             (from (as (-> (select :id (as '(- (/ close ((lag :close) over (partition by :quote-id order by :date desc))) 1)
                                           :daily-return))
                           (from :prices)
                           (where `(= :prices.quote-id ~(:id quote)))) :u))
             (where `(and (= :prices.id :u.id)
                          (= :prices.quote-id ~(:id quote))))))
       [(str "UPDATE prices SET daily-return = u.daily-return "
             "FROM (SELECT id, ((close / lag(close) over (partition by quote-id order by date desc)) - 1) AS daily-return "
             "FROM prices WHERE (prices.quote-id = 1)) AS u WHERE ((prices.id = u.id) and (prices.quote-id = 1))")]
       (-> (update
               :airports
               '((= :country-id :u.id)
                 (= :gps-code :u.gps-code)
                 (= :wikipedia-url :u.wikipedia)
                 (= :location :u.geom)))
           (from (-> (select (distinct [:c.id :a.name :a.gps-code :a.iata-code :a.wikipedia :a.geom] :on [:a.iata-code]))
                     (from (as :natural-earth.airports :a))
                     (join (as :countries :c) '(on (:&& :c.geography :a.geom)))
                     (join :airports '(on (= (lower :airports.iata-code) (lower :a.iata-code))) :type :left)
                     (where '(and (is-not-null :a.gps-code)
                                  (is-not-null :a.iata-code)
                                  (is-not-null :airports.iata-code)))
                     (as :u)))
           (where '(= :airports.iata-code :u.iata-code)))
       [(str "UPDATE airports SET country-id = u.id, gps-code = u.gps-code, wikipedia-url = u.wikipedia, location = u.geom "
             "FROM (SELECT DISTINCT ON (a.iata-code) c.id, a.name, a.gps-code, a.iata-code, a.wikipedia, a.geom "
             "FROM natural-earth.airports AS a LEFT JOIN airports ON (lower(airports.iata-code) = lower(a.iata-code)) "
             "JOIN countries AS c ON (c.geography && a.geom) WHERE ((a.gps-code IS NOT NULL) and "
             "(a.iata-code IS NOT NULL) and (airports.iata-code IS NOT NULL))) AS u WHERE (airports.iata-code = u.iata-code)")]))
