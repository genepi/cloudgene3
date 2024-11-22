### Taxprofiler Pipeline with Dataset Integration Using Cloudgene

In this tutorial, we will adapt the `taxprofiler` pipeline for taxonomic classification and profiling of metagenomic data using Cloudgene. The pipeline will be configured to integrate with datasets, allowing users to select specific datasets for their analysis while reducing input complexity. The Cloudgene application will be tailored for this use case, simplifying the pipeline's input parameters and linking datasets dynamically.

---

### Taxprofiler Application

We start with the `taxprofiler` pipeline, which fetches data based on user-provided input IDs and performs taxonomic profiling. Here is the `cloudgene.yaml` file for the `taxprofiler` pipeline:

```yaml
id: taxprofiler
name: Taxprofiler
description: Taxonomic classification and profiling of shotgun short- and long-read metagenomic data
version: 1.1.8
website: https://github.com/nf-core/taxprofiler
author: James A. Fellows Yates, Sofia Stamouli, Moritz E. Beber, and the nf-core/taxprofiler team
logo: https://raw.githubusercontent.com/nf-core/fetchngs/master/docs/images/nf-core-fetchngs_logo_light.png

workflow:
  steps:
    - name: Fetch Data
      script: nf-core/fetchngs
      revision: 1.12.0
      stdout: true
      params:
        input: "${input_ids}"

    - name: Run taxprofiler
      script: nf-core/taxprofiler
      revision: 1.1.8
      stdout: true
      params:
        input: "${outdir}/samplesheet/samplesheet.csv"
        databases: "https://raw.githubusercontent.com/nf-core/test-datasets/taxprofiler/database_full_v1.2.csv"
        multiqc_title: "${CLOUDGENE_JOB_NAME}"

  inputs:
    - id: input_ids
      description: IDs
      type: textarea
      value: "SRR12696236"
      writeFile: "ids.csv"
      serialize: false

  outputs:
    - id: outdir
      description: Output
      type: folder
```

This setup fetches sequence data and runs the taxprofiler pipeline using an external database. However, we want to make the database selection dynamic and allow users to choose a specific dataset for taxonomic profiling. 

---

### Datasets for Taxprofiler

Datasets allow us to link the pipeline to different database options dynamically, enabling users to select the most relevant database for their analysis. We will create individual Cloudgene applications for each database dataset and link them to the `taxprofiler` pipeline.

#### Dataset Structure

We will create a Cloudgene application for each database with the following structure:

```ansi
database1
├── cloudgene.yaml
├── database_full_v1.2.csv
└── README.md
```

#### `cloudgene.yaml` for Dataset 1

The `cloudgene.yaml` file for each dataset application will define properties to share the dataset location with the main pipeline:

```yaml
id: db1
name: Database 1
version: 1.0
category: taxprofiler_database
properties:
  database_url: "${CLOUDGENE_APP_LOCATION}/database_full_v1.2.csv"
```

Here, the `database_url` property points to the dataset file, allowing the main pipeline to retrieve this information when the user selects the dataset. The `category` field is used to group all dataset applications, making it easier to filter and display them within the `taxprofiler` pipeline interface.

---

### Updating the Taxprofiler Application to Use Datasets

Now, we update the `cloudgene.yaml` file for the `taxprofiler` pipeline to use datasets. Instead of hardcoding the database URL, we will allow users to select from available datasets.

#### Updated `cloudgene.yaml` for Taxprofiler

```yaml
id: taxprofiler
name: Taxprofiler
description: Taxonomic classification and profiling of shotgun short- and long-read metagenomic data
version: 1.1.8

workflow:
  steps:
    - name: Fetch Data
      script: nf-core/fetchngs
      revision: 1.12.0
      stdout: true
      params:
        input: "${input_ids}"

    - name: Run taxprofiler
      script: nf-core/taxprofiler
      revision: 1.1.8
      stdout: true
      params:
        input: "${outdir}/samplesheet/samplesheet.csv"
        databases: "${database.database_url}"
        multiqc_title: "${CLOUDGENE_JOB_NAME}"

  inputs:
    - id: input_ids
      description: IDs
      type: textarea
      value: "SRR12696236"
      writeFile: "ids.csv"
      serialize: false
    - id: database
      description: Database
      type: dataset
      category: taxprofiler_database
      serialize: false

  outputs:
    - id: outdir
      description: Output
      type: folder
```

#### Explanation

- **`dataset` input**: The input `database` uses the `dataset` type, allowing users to select a dataset from all installed database applications that fall under the `taxprofiler_database` category.
- **Database link**: The `database.database_url` references the `database_url` property defined in the dataset application’s `cloudgene.yaml`, dynamically linking the selected database to the `taxprofiler` pipeline.
  
With this setup, users can select a dataset from the available options, and the `taxprofiler` pipeline will use the corresponding database for taxonomic profiling.

---

### Advantages of This Approach

1. **Dynamic Dataset Selection**: Users can easily select a dataset from a predefined list without modifying the pipeline configuration.
2. **Separation of Concerns**: The logic of dataset management is separated from the pipeline itself, making it easier to add or update datasets without changing the pipeline code.
3. **Scalability**: As new datasets are added, they are automatically available for selection without further changes to the pipeline.
4. **Permission Management**: Admins can control access to specific datasets through user groups, providing flexibility in shared environments.

By using this approach, the `taxprofiler` pipeline becomes more user-friendly and adaptable, enabling seamless integration of datasets for specialized use cases.