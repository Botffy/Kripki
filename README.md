Kelleni fog [Git](http://git-scm.com/downloads) és [Apache Ant](http://ant.apache.org/bindownload.cgi): ezeket töltsük le és telepítsük.
A repository-t a `git clone git@github.com:Botffy/Kripki.git` parancs kiadásával klónozzuk a jelenlegi könyvtárba, majd lépjünk be a kapott Kripki könyvtárba.
Ha nincs [Apache Ivy](http://ant.apache.org/ivy/) telepítve, akkor az `ant bootstrap` paranccsal telepítsük; majd `ant resolve` paranccsal telepítsük a függőségeket.

Ezután:

* `ant build` parancsra fordul (kliens és szerver is)
* `ant shipit` parancsra futtatható jar készül a szerverből és a kliensből is (a dist könyvtárban)
* `ant shipit-pro` parancsra optimalizált futtatható jarok készülnek (kell hozzá ProGuard)
* `ant runserver` parancsra indul a szerver
* `ant runclient` parancsra indul a kliens
* `ant run-more-clients`-re két versengő kliens indul.
* `ant make-runscripts`-re futattóscriptek készülnek, amelyekkel könnyen lehet átadni paramétert a futtatandó programoknak
