
## Covid19 live colombian reports

Running Clojurescript and Clojure to fetch data from colombian open
data repository (datos.gov.co)

This project is published on github pages, the web report can be found here:

https://papachan.github.io/data-covid19-colombia/

### Data repository

* You will find latest CSV File with INS Data here:

https://github.com/papachan/data-covid19-colombia/tree/master/data

* A daily report using json format for charts:

https://papachan.github.io/data-covid19-colombia/datos.json

Complete version (5 mb)

https://raw.githubusercontent.com/papachan/data-covid19-colombia/master/resources/datos.json

* Timeseries Deaths and Cases (Json format)

https://papachan.github.io/data-covid19-colombia/timeseries.json

* Number of Covid Tests (json format)

https://papachan.github.io/data-covid19-colombia/covid-tests.json


### URLs and related links

Colombia publish every day different reports from the INS (Colombian
National Health Institute) and store them under datos.gov.co platform.

* [Datos Ocupación UCI](http://saludata.saludcapital.gov.co/osb/index.php/datos-de-salud/enfermedades-trasmisibles/ocupacion-ucis/)
* [Coronavirus Reporte del Instituto Nacional de la Salud](https://www.ins.gov.co/Noticias/Paginas/Coronavirus.aspx)
* [Dataset de los casos de COVID-19 en Colombia](https://www.datos.gov.co/Salud-y-Protecci-n-Social/Casos-positivos-de-COVID-19-en-Colombia/gt2j-8ykr/data)


### Build and development

Dev environment with Clojure ( to load crawler and downloads features ):

```
    clj -A:repl
```

You can lanch your option commands: `crawl` `clean` `export` /
`download` and `update` as this:

```
    clj -A:main [your option]
```

#### Clojurescript App:


Start project:

```
    yarn
```

Run into dev mode:

```
    shadow-cljs -A:shadow-cljs watch app
```

Release a new build for github pages:


```
    shadow-cljs -A:release release app
```


### Copyright

Copyright (C) 2020 Dan Loaiza <papachan@gmail.com>
