# BashCommand step

Cloudgene supports the execution of executable binaries.

## Parameters

| Parameter | Required | Description                                                                        |
| --- | --- |------------------------------------------------------------------------------------|
| `type` | yes | Type has to be `command`                                                           |
| `cmd` | yes | The command that should be executed                                                |
| `bash` | no | Enables or disable Bash specific features like pipes and loops (default: **false**) |
| `stdout` | no | Use stdout as step's output (default: **false**)                                   |

## Examples

### Print message to using `/bin/echo`

This example shows how to forward stdout directly to the output of a step in order to display it the web-application.


```yaml
id: cmd-example
name: Command Example
version: 1.0
workflow:
  steps:
    - name: Print text to terminal
      type: command
      cmd: /bin/echo $message
      stdout: true
  inputs:
    - id: message
      description: Message
      type: text
```


### Write message to a file using stdout streaming

This example shows how to use Bash specific features by setting the `bash` property to `true`:

```yaml
id: bash-example
name: Bash Example
version: 1.0
workflow:
  steps:
    - name: Write text to file using pipes
      type: command
      cmd: /bin/echo $message > $output
      bash: true
  inputs:
    - id: message
      description: Message
      type: text
  outputs:
    - id: output
      description: Output File
      type: local_file
```