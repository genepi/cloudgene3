# Getting started

This guide helps you to start Cloudgene and to install your first application. You have to install Cloudgene properly on your computer before you can start.

## Start Cloudgene Server

The webserver can be started with the following command:

```sh
./cloudgene server
```

The webservice is available on [http://localhost:8082](http://localhost:8082). Please use username `admin` and password `admin1978` to login. 
The default port can be changed in the [configuration file](configuration.md#web-application).

## Install your first application

Stop the webservice by pressing `CTRL-C`. The **cg-fetchngs** application can be installed by using the following command:

```sh
./cloudgene install lukfor/cg-fetchngs
```

Next, restart Cloudgene with the following command:

```sh
./cloudgene server
```

Open Cloudgene in your browser and login. A new menu item **Run** appears in the menubar. Click on it to start a new job:

<div class="screenshot">
<img src="../../images/screenshots/menubar.png">
<img src="../../images/screenshots/hello-cloudgene.png">
</div>

The **hello-cloudgene** application displays several inspiring quotes:

<div class="screenshot">
<img src="../../images/screenshots/hello-cloudgene-results.png">
</div>

## What's next?

- [Install additional applications](/server/install-apps)
- [Configure and customize Cloudgene](/server/configuration) to support E-Mail notification, SSL cerificates,
- Learn how to [manage permissions](/server/permissions) and [handle jobs](/daemon/jobs)
