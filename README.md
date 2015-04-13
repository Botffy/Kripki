Kriptográfia beadandó

Kelleni fog [Apache Ant](http://ant.apache.org/bindownload.cgi).
Ha nincs [Apache Ivy](http://ant.apache.org/ivy/) telepítve, akkor az `ant bootstrap` paranccsal telepítsük; majd `ant resolve` paranccsal telepítsük a függőségeket.

* `ant build` parancsra fordul (kliens és szerver is)
* `ant shipit` parancsra futtatható jar készül a szerverből és a kliensből is (a dist könyvtárban)
* `ant runserver` parancsra indul a szerver
* `ant runclient` parancsra indul a kliens
* `ant run-more-clients`-re két versengő kliens indul.
