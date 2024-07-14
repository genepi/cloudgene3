# Nextflow

Cloudgene supports the execution of Nextflow pipelines.


Cloudgene automatically creates a user interface with input parameters. Upon submission, it generates the outputs (folders or files). All inputs and outputs are automatically added to the params.json file, which Cloudgene uses to execute the Nextflow workflow.

## Parameters

| Parameter | Required | Description                                                                        |
|-----------| --- |------------------------------------------------------------------------------------|
| `type`    | yes | Type has to be `nextflow`                                                          |
| `script`  | yes |
| `params`  | no |                                   |

## Examples


- all 