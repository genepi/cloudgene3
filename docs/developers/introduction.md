# Introduction

This guide will walk you through creating a Cloudgene YAML file that acts as a bridge between the Nextflow pipeline and Cloudgene. We'll create a Cloudgene YAML file, define workflow steps, inputs, and outputs, and demonstrate how to set default parameters.

## Prerequisites

Before you begin, ensure you have the following:

- Cloudgene installed
- Nextflow installed
- Basic understanding of YAML syntax

## Creating the Cloudgene YAML File

The Cloudgene YAML file defines the link between the Nextflow pipeline and Cloudgene. It contains metadata about the pipeline, the workflow steps, inputs, and outputs.

### Header Section

The header section includes basic information about the pipeline:

```yaml
id: fetch-ngs
name: FetchNGS
description: Pipeline to fetch metadata and raw FastQ files from public databases
version: 1.12.0
website: https://github.com/nf-core/fetchngs
author: Harshil Patel, Moritz E. Beber and Jose Espinosa-Carrasco
logo: https://raw.githubusercontent.com/nf-core/fetchngs/master/docs/images/nf-core-fetchngs_logo_light.png
```

### Workflow Section

The workflow section defines the steps, inputs, and outputs.

#### Defining the Workflow Object

In the workflow object, we define a step that executes the `nf-core/fetchngs` pipeline at version 1.12.0:

```yaml
workflow:
  steps:
    - name: Fetch NGS
      script: nf-core/fetchngs
      revision: 1.12.0
```

#### Defining Inputs and Outputs

The pipeline has one input and one output. We define the corresponding variables and their types.

**Example with File Input:**

```yaml
inputs:
  - id: input
    description: ID File
    type: file

outputs:
  - id: outdir
    description: Output
    type: folder
```

Cloudgene automatically creates a user interface with input parameters. Upon submission, it generates the outputs (folders or files). All inputs and outputs are automatically added to the `params.json` file, which Cloudgene uses to execute the Nextflow workflow.

### Extended Example with Textarea Input

We can extend the configuration to allow users to enter a list of IDs in a textarea. Cloudgene writes this content to a file by setting the flag `writeFile`. We can also set a default value with `value`.

Here’s a complete example combining all the sections:

```yaml
id: fetch-ngs
name: FetchNGS
description: Pipeline to fetch metadata and raw FastQ files from public databases
version: 1.12.0
website: https://github.com/nf-core/fetchngs
author: Harshil Patel, Moritz E. Beber and Jose Espinosa-Carrasco
logo: https://raw.githubusercontent.com/nf-core/fetchngs/master/docs/images/nf-core-fetchngs_logo_light.png

workflow:
  steps:
    - name: Fetch NGS
      script: nf-core/fetchngs
      revision: 1.12.0
      params:
        monochrome_logs: false

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

You can save the content in a file named `cloudgene.yaml` and install it with the following command:

```bash
cloudgene install /path/to/cloudgene.yaml
```

Once the webserver is started using `cloudgene server`, you can open the web interface to run the application.

If you update the `cloudgene.yaml` file, you need to go to the Admin Panel -> Applications and click on "Reload Application" for the changes to take effect.

### Setting Default Parameters

We can also set default parameters without requiring user inputs:

```yaml
workflow:
  steps:
    - name: Fetch NGS
      script: nf-core/fetchngs
      revision: 1.12.0
      params:
        monochrome_logs: false
```

### Excluding Parameters from `params.json`

In Cloudgene, all inputs and outputs are automatically added to the `params.json` file. This file is essential for Cloudgene to execute the Nextflow workflow, as it contains all parameter mappings required for the run. However, there are scenarios where it may be necessary to exclude certain parameters from being serialized into `params.json`.

For example, you might have intermediate variables or temporary configurations that are not essential for the final execution, that you don’t want to be serialized to the `params.json` file.

To exclude a parameter from being added to `params.json`, you can use the `serialize` flag. By setting `serialize: false` for a specific parameter, you ensure that it won’t be written into the JSON file.

In the following example, a parameter `profiler` is defined for internal use, but we don’t want it to be included in the `params.json` file:

```yaml
workflow:
  inputs:
    - id: input
      description: Samplesheet
      type: file
    - id: profiler
      description: Profiler
      type: list
      value: kraken2
      values:
        kraken2: Kraken 2
        metaphlan: MetaPhlAn
      serialize: false
```

By setting `serialize: false`, the `profiler` will be used in the workflow, but it won’t appear in the `params.json` file. We can use the `params` section in a step to remap the `profiler` to different pipeline parameters. For example:

```yaml
workflow:
  steps:
    - name: Run taxprofiler
      script: nf-core/taxprofiler
      revision: 1.1.8
      params:
        input: "${input}"
        run_kraken2: ${{profiler == "kraken2"}}
        run_metaphlan: ${{profiler == "metaphlan"}}
        databases: "https://raw.githubusercontent.com/nf-core/test-datasets/taxprofiler/database_full_v1.2.csv"
```

## Local Nextflow Script

You can also place the `cloudgene.yaml` file directly into your pipeline directory and set the `script` property to the Nextflow script (e.g., `main.nf`). In this case, if you install the application via GitHub, Cloudgene automatically downloads the pipeline and executes the script.

```yaml
workflow:
  steps:
    - name: Execute Pipeline
      script: main.nf
```

## Connecting Pipelines

Cloudgene supports the connection of multiple pipelines by allowing you to define workflows that consist of multiple steps. Each step can execute a separate pipeline, and the output of one pipeline can be passed as the input to the next.

In this example, Cloudgene connects two Nextflow pipelines (nf-core/fetchngs and nf-core/taxprofiler) into a single workflow, making it easy to fetch data and then run a taxonomic profiler.

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

The `cloudgene.yaml` file is hosted on GitHub. You can simply install the application with the following command:

```yaml
cloudgene install lukfor/cg-taxprofiler
```

## Conclusion

You have now created a Cloudgene YAML file that defines a Nextflow pipeline workflow with inputs, outputs, and default parameters. This configuration allows Cloudgene to generate a user interface, handle inputs and outputs, and execute the Nextflow workflow seamlessly.