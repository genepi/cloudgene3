# Reporting in Cloudgene

Cloudgene supports reporting by parsing the standard output (stdout) of each process it runs. This allows users to generate detailed logs and reports using specific output commands. The functionality is similar to GitHub Actions, utilizing annotations and groups to organize and present information effectively.

## Example

=== ":material-file: main.nf"
    ```groovy
    params.n = 5 // Default number of quotes to extract

    process extractRandomQuote {
        input:
        path quotesFile
        val index
    
        script:
        """
        QUOTE=`shuf ${quotesFile} | head -n 1`
        echo "::message::\${QUOTE}"
        """
    }
    
    workflow {
        extractRandomQuote(
            file("${projectDir}/quotes.txt"),
            Channel.of(1..params.n)
        )
    }
    ```

=== ":material-file: cloudgene.yaml"
    ```groovy
    id: hello-cg
    name: Hello CG
    version: 1.0
    workflow:
    steps:
    - name: Get quotes
      script: main.nf
    ```

=== ":material-image: Output"
    ![reporting.png](/images/reporting.png)

## Output Commands

Cloudgene recognizes a series of specific commands in the stdout that help classify and structure log messages. Below is a list of supported commands and their usage:

### Error Messages
To log an error message:
```bash
echo "::error::This is an error message"
```

### General Messages
To log a general message:
```bash
echo "::message::This is a message"
```

### Notices
To log a notice message:
```bash
echo "::notice::This is a notice message"
```

### Warnings
To log a warning message:
```bash
echo "::warning::This is a warning message"
```

### Debug Messages
To log a debug message:
```bash
echo "::debug::This is a debug message"
```

## Grouping Messages

Cloudgene allows grouping of related log messages to enhance readability. Groups can have a specified type (e.g., error, warning) to provide additional context. Use the following commands to start and end a group:

### Start a Group
To start a group:
```bash
echo "::group type=error::"
```
This will group subsequent log messages until the group is closed.

### End a Group
To end a group:
```bash
echo "::endgroup::"
```

### Example Group
```bash
echo "::group type=error::"
echo "This is a list:"
echo "- Line 1"
echo "- Line 2"
echo "::endgroup::"
```
In this example, the messages "This is a list:", "- Line 1", and "- Line 2" will be grouped together under an error type.

## Counters

Cloudgene supports setting and submitting counters, which can be useful for tracking various metrics during the execution of a process.

### Set a Counter
To set a counter with a specific name and value:
```bash
echo "::set-counter name=genomes::55"
```

### Submit a Counter
To submit a counter, which can then be used for further processing or reporting:
```bash
echo "::submit-counter name=genomes::"
```

