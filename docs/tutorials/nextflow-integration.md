# Getting Started with Cloudgene and Nextflow Pipeline Integration

This guide will walk you through creating a Cloudgene YAML file that acts as a bridge between the Nextflow pipeline and Cloudgene. We'll create a Cloudgene YAML file, define workflow steps, inputs, and outputs, and demonstrate how to set default parameters.

## Prerequisites

Before you begin, ensure you have the following:

- Cloudgene installed
- Nextflow installed
- Basic understanding of YAML syntax

## Step 1: Creating the Cloudgene YAML File

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
      type: nextflow
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
    type: local_file

outputs:
  - id: outdir
    description: Output
    type: local-folder
```

Cloudgene automatically creates a user interface with input parameters. Upon submission, it generates the outputs (folders or files). All inputs and outputs are automatically added to the `params.json` file, which Cloudgene uses to execute the Nextflow workflow.

### Extended Example with Textarea Input

We can extend the configuration to allow users to enter a list of IDs in a textarea. Cloudgene writes this content to a file.

**Example with Textarea Input:**

```yaml
workflow:
  steps:
    - name: Fetch NGS
      type: nextflow
      script: nf-core/fetchngs
      revision: 1.12.0

  inputs:
    - id: input
      description: IDs
      type: textarea
      writeFile: "ids.csv"

  outputs:
    - id: outdir
      description: Output
      type: local-folder
```

### Setting Default Parameters

We can also set default parameters without requiring user inputs.

**Example with Default Parameters:**

```yaml
workflow:
  steps:
    - name: Fetch NGS
      type: nextflow
      script: nf-core/fetchngs
      revision: 1.12.0
      params:
        monochrome_logs: false
```

## Complete Example

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
      type: nextflow
      script: nf-core/fetchngs
      revision: 1.12.0
      params:
        monochrome_logs: false

  inputs:
    - id: input
      description: IDs
      type: textarea
      writeFile: "ids.csv"

  outputs:
    - id: outdir
      description: Output
      type: local-folder
```

## Conclusion

You have now created a Cloudgene YAML file that defines a Nextflow pipeline workflow with inputs, outputs, and default parameters. This configuration allows Cloudgene to generate a user interface, handle inputs and outputs, and execute the Nextflow workflow seamlessly.

For more information, refer to the [Cloudgene documentation](https://cloudgene.readthedocs.io/) and the [nf-core/fetchngs repository](https://github.com/nf-core/fetchngs).