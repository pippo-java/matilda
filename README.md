Web Accounting - Pippo Demo 
=====================
[![Travis CI Build Status](https://travis-ci.org/decebals/matilda.png)](https://travis-ci.org/decebals/matilda)

This project is a demo application build with [Pippo](www.pippo.ro) framework.
The goal of this project is to show how looks a real web application build with Pippo.

before studying this project please read about Pippo on www.pippo.ro and see the multiple Pippo-Demo projects (fine granularity) on https://github.com/decebals/pippo-demo.

Libraries used
-------------------
This project is build using several libraries:
- [Pippo](https://github.com/decebals/pippo), a micro Java web framework
- [Dada](https://github.com/decebals/dada), a tiny generic dao in Java used to access data stored in a H2 database
- [Pebble](https://github.com/mbosecke/pebble), a Java templating engine
- [Undertow](https://github.com/undertow-io/undertow), high performance non-blocking webserver
- [Bootstrap](https://github.com/twbs/bootstrap)
- [Bootstrap FileInput](https://github.com/kartik-v/bootstrap-fileinput)
- [Bootstrap Datepicker](https://github.com/eternicode/bootstrap-datepicker)
- [Bootstrap Validator](https://github.com/1000hz/bootstrap-validator)
- [Bootstrap3 Dialog](https://github.com/nakupanda/bootstrap3-dialog)
- [IntercoolerJs](https://github.com/LeadDyno/intercooler-js)
- [ListJs](https://github.com/javve/list.js)

About business
-------------------
This application is a web accounting application where the customers upload invoices and the 
accountant processes these invoices and upload the balance sheet.
The customer has a company and he can not upload documents (PDF files) after day 20 of each month without the accept of the accountant. Also he can not upload documents in system if he forgot to pay on the previous month.

The application comes with two sections: __Customer__ and __Admin__ (Accountant). Each section comes with a separate authentication mechanism. The authentication mechanism for Customer is reinforced with Google reCAPCTCHA and the authentication mechanism for Admin is reinforced with an IP White List.

The application comes with support for `English` and `Romanian` languages.

Screenshots
-------------------
Some screenshots are available in the `screenshots` folder.

How to run
-------------------
- `mvn package`
- `mkdir dist`
- `cd dist`
- `unzip ../target/matilda-0.1.0-SNAPSHOT.zip`
- `java -jar matilda-0.1.0-SNAPSHOT.jar`

With the last command the server (Undertow) started on port `8338` so open your favorite browser and type `http://localhost:8338` (email/password: test@test.ro/1) or for admin section `http://localhost:8338/admin` (username/password: test/1).

How to build
-------------------
Requirements:
- [Git](http://git-scm.com/)
- JDK 8 (test with `java -version`)
- [Apache Maven 3](http://maven.apache.org/) (test with `mvn -version`)

Steps:
- create a local clone of this repository (with `git clone https://github.com/decebals/matilda.git`)
- go to project's folder (with `cd matilda`)
- build the artifacts (with `mvn clean package`)

After above steps a folder _matilda/target_ is created and all goodies are in that folder.

Versioning
------------
Matilda will be maintained under the Semantic Versioning guidelines as much as possible.

Releases will be numbered with the follow format:

`<major>.<minor>.<patch>`

And constructed with the following guidelines:

* Breaking backward compatibility bumps the major
* New additions without breaking backward compatibility bumps the minor
* Bug fixes and misc changes bump the patch

For more information on SemVer, please visit http://semver.org.
