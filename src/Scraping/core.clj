(ns Scraping.core
  (:use [clj-webdriver.taxi]
        [clojure.string :only [split blank? trim]]))

(defn log-voucher [driver code]
  (do
    (println "Voucher:" (format "%05d" code))
    (println "Note:" (text driver "li.help"))))

(defn log-invalid-voucher [driver code]
  (println "Voucher:" (format "%05d" code) "was invalid"))

(defn remove-added-items [driver]
  (do
    (doall (map #(toggle driver %1) (elements driver ".order-details-checkbox>input")))
    (execute-script driver "document.getElementById('cmdRemoveItems').setAttribute('onClick', 'true')")
    (click driver "#cmdRemoveItems")))

(defn process-code [driver code]
  (do
    (clear driver "#txtVoucher")
    (input-text driver "#txtVoucher" (format "%05d" code))
    (click driver "#cmdAdd")
    (if (exists? driver ".order-details-checkbox>input") (do (log-voucher driver code) (remove-added-items driver)) (log-invalid-voucher driver code))))

(defn login [driver]
  (do 
    (to driver "https://internetorder.dominos.com.au/Accessible/Default.aspx")
    (click driver "#lnkPickup")
    (input-text driver "#txtCustomerName", "Pizza Codes")
    (input-text driver "#txtPhoneNumber", "0123456789")
    (input-text driver "#txtPickupStore", "Woollongabba")
    (input-text driver "#txtEmail", "email@email.com")
    (click driver "#cmdSubmit")
    (click driver "a.address-result")
    (click driver "#cmdSubmit")
    (if (exists? driver "#lnkPickupAsap")
      (click driver "#lnkPickupAsap")
      (do (select-by-index driver "#cboOrderTime" 2) (click driver "#cmdSubmit")))))

(defn scrape [driver codes]
  (do
    (login driver)
    (doall (for [code codes] (process-code driver code)))))

(defn add-pizza [driver]
  (do
    (click driver (second (elements driver "#productGroupMenu>li>a")))
    (click driver (nth (elements driver ".product-list>li>a") 7))
    (click driver "#cmdAddToBasket")))

(defn check-current-cost [driver] 
  (println "Cost:" (text driver "#total>span")))

(defn check-price [driver code]
  (do
    (println "Voucher:" (format "%05d" (read-string code)))
    (clear driver "#txtVoucher")
    (input-text driver "#txtVoucher" (format "%05d" (read-string code)))
    (click driver "#cmdAdd")
    (add-pizza driver)
    (check-current-cost driver)
    (remove-added-items driver)))

(defn check-code [driver code]
  (let [values (remove blank? (split code #":"))
        voucher (trim (first (split (first values) #"\n")))
        note (trim (first (split (second values) #"\n")))]
       (if (= note "Traditional Pizzas") (check-price driver voucher))))

(defn check-codes [driver]
  (do
    (login driver)
    (doall
      (map #(check-code driver %1)
           (remove blank? (split (slurp "total.txt") #"Voucher"))))))

(defn -main
  [& args]
  (scrape (new-driver {:browser :firefox}) (range 0 100000)))
;  (dotimes [i 10] (.start (Thread. (fn [] (scrape (new-driver {:browser :firefox}) (range (* i 10000) (* (+ i 1) 10000))))))))