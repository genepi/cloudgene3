# Outputs

Output parameters can be used as placeholders for folders or files which are created by steps. Cloudgene supports POSIX compatible filesystems (e.g. Linux or OS X) and the *Hadoop Distributed File System* (HDFS). Cloudgene creates and manages these folders for you as well as enables downloading the files through the web interface.

Output parameters are defined in the `outputs` section where each parameter is defined by an unique `id`, a textual `description` and a `type`.

```yaml hl_lines="6 7 8 9 10 11"
id: output-example
name: Output Example
version: 1.0
workflow:
  outputs:
    - id: output
      description: Output Folder
      type: local_folder
      mergeOutput: false
      download: true
      zip: false
```

The value of the parameter can be referenced by `$id` in the workflow. In this example, we use the `touch` command to create a file in an output folder:

```yaml hl_lines="7"
id: output-example
name: Output Example
version: 1.0
workflow:
  steps:
    - name: Name Step1
      cmd: /bin/touch $output/new-file.txt
  outputs:
    - id: output
      description: Output Folder
      type: local_folder
      mergeOutput: false
      download: true
      zip: false
```

## Properties

These properties define the basic behaviour of an output parameter:

| Property | Required | Description |
| ---- | --- | --- |
| `id` | yes | Defines a id for the parameter |
| `description` | yes | Defines a description for the parameter |
| `type` | yes | One of the following [types](/developers/outputs/#types) |
| `download` | no | If `download` is set to true, the file or folder can be downloaded (default: **true**). |
| `adminOnly` | no | (default: **false**). |

- Add serialize
- Add includes/excludes

## Types

At the moment the following types of output parameters are supported:

### `local_file`

### `local_folder`