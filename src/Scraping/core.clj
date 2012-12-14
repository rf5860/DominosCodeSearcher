(ns Scraping.core)
(use 'clj-webdriver.taxi)

(defn log-voucher [driver code]
  (do
    (println "Voucher:" code)
    (println "Note:" (text driver "li.help"))))

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
    (if (exists? driver ".order-details-checkbox>input") (do (log-voucher driver code) (remove-added-items driver)))))

(defn scrape [driver codes]
  (do
    (to driver "https://internetorder.dominos.com.au/Accessible/Default.aspx")
    (click driver "#lnkPickup")
    (input-text driver "#txtCustomerName", "Pizza Codes")
    (input-text driver "#txtPhoneNumber", "0123456789")
    (input-text driver "#txtPickupStore", "Store")
    (input-text driver "#txtEmail", "email@email.com")
    (click driver "#cmdSubmit")
    (click driver "a.address-result")
    (click driver "#cmdSubmit")
    (click driver "#lnkPickupAsap")
    (doall (for [code codes] (process-code driver code)))))

(defn -main
  [& args]
  (dotimes [i 10] (.start (Thread. (fn [] (scrape (new-driver {:browser :firefox}) (range (* i 10000) (* (+ i 1) 10000))))))))