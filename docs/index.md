---
hide:
  - navigation
  - toc 
---
<div class="header" markdown="1">

# Cloudgene 3

## Turn Your Nextflow Pipeline into a Powerful Web Service


[:fontawesome-solid-book: Getting Started](server/introduction.md){ .md-button .md-button} [:fontawesome-solid-download: Installation](installation.md){ .md-button } [:fontawesome-brands-github: Source](https://github.com/genepi/cloudgene){ .md-button }
</div>

---

- **Build** your analysis pipeline in Nextflow
- **Integrate** your analysis pipeline into Cloudgene by writing a simple configuration file
- **Get** a powerful web application with user management, data transfer, error handling and more
- **Deploy** your application with one click to in-house clusters or public Clouds like Amazon AWS
- **Offer** your application as SaaS to other scientists, managing thousands of jobs like a pro
- **Share** your application, enabling others to clone your service to their own hardware or private cloud instance

---

## Integrate your Nextflow pipeline

=== ":material-file: cloudgene.yaml"

    ```yaml
    id: fetch-ngs
    name: FetchNGS
    description: Pipeline to fetch metadata and raw FastQ files from public databases
    version: 1.12.0
    website: https://github.com/nf-core/fetchngs
    workflow:
      steps:
        - name: Fetch NGS
          script: nf-core/fetchngs
          revision: 1.12.0
    
      inputs:
        - id: input
          description: IDs
          type: textarea
          value: "SRR12696236"
          writeFile: "ids.csv"
    
      outputs:
        - id: outdir
          description: Output
          type: local-folder
    ```


---

## Who uses Cloudgene?


### Michigan Imputation Server



### mtDNA-Server

---

## Citation

---

## About

Cloudgene has been created by [Lukas Forer](https://twitter.com/lukfor) and [Sebastian Schönherr](https://twitter.com/seppinho) and is MIT Licensed.


[![@lukfor](https://avatars.githubusercontent.com/u/210220?s=64&v=4)](https://github.com/lukfor)
[![@seppinho](https://avatars.githubusercontent.com/u/1942824?s=64&v=4)](https://github.com/seppinho)

Thanks to all the [contributors](about.md) to help us maintaining and improving Cloudgene!
