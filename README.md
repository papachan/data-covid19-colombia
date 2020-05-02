
## Covid19 live colombian reports

Running Clojurescript and Clojure to fetch data from colombian open
data repository (datos.gov.co)

This project is published on github pages you can find it here:

https://papachan.github.io/data-covid19-colombia/

## Data repository

You will find latest CSV File with INS Data at https://github.com/papachan/data-covid19-colombia/tree/master/data

Or in json format you can use this file:

https://raw.githubusercontent.com/papachan/data-covid19-colombia/master/resources/datos.json

### URLs and related links

**"Casos positivos de COVID-19 en Colombia"**

Colombia publish every day different reports from INS (Colombian
Health National Institute).

* https://datosabiertos.bogota.gov.co/dataset
* https://www.datos.gov.co/Salud-y-Protecci-n-Social/Casos-positivos-de-COVID-19-en-Colombia/gt2j-8ykr/data
* [colombian report at infogram](https://infogram.com/covid-19-or-instituto-nacional-de-salud-or-colombia-1hke60w3qlz345r)


## Build and development

Start project:

```
    yarn
```

Run into dev mode:

```
    shadow-cljs watch app
```

Release a new build for github pages:


```
    shadow-cljs release app
```


### Copyright

Copyright (C) 2020 Dan Loaiza <papachan@gmail.com>
