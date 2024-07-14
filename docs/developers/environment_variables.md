# Environment Variables

Cloudgene supports several environment variables that can be used in your `cloudgene.yaml` files to get informations about the application itself and the submitted job.

### Application

| Variable                    | Description                                      |
|-----------------------------|--------------------------------------------------|
| `${CLOUDGENE_APP_LOCATION}` | The local folder path of the application.        |


### Job

| Variable                    | Description                                      |
|-----------------------------|--------------------------------------------------|
| `${CLOUDGENE_JOB_ID}`       | The ID of the job.                               |
| `${CLOUDGENE_JOB_NAME}`     | The name of the job.                             |


### User

| Variable                    | Description                                      |
|-----------------------------|--------------------------------------------------|
| `${CLOUDGENE_USER_NAME}`    | The username of the user.                        |
| `${CLOUDGENE_USER_EMAIL}`   | The email address of the user.                   |
| `${CLOUDGENE_USER_FULL_NAME}`| The full name of the user.                      |


### Service

| Variable                    | Description                                      |
|-----------------------------|--------------------------------------------------|
| `${CLOUDGENE_SERVICE_NAME}` | The name of the service.                         |
| `${CLOUDGENE_SERVICE_URL}`  | The full URL of the service.                     |
| `${CLOUDGENE_CONTACT_EMAIL}`| The contact email of the service administrator.  |
| `${CLOUDGENE_CONTACT_NAME}` | The contact name of the service administrator.   |
| `${CLOUDGENE_SMTP_HOST}`    | The SMTP host for email services.                |
| `${CLOUDGENE_SMTP_PORT}`    | The SMTP port for email services.                |
| `${CLOUDGENE_SMTP_USER}`    | The SMTP user for email services.                |
| `${CLOUDGENE_SMTP_PASSWORD}`| The SMTP password for email services.            |
| `${CLOUDGENE_SMTP_NAME}`    | The SMTP name for email services.                |
| `${CLOUDGENE_SMTP_SENDER}`  | The SMTP sender name for email services.         |
| `${CLOUDGENE_WORKSPACE_TYPE}`| The type of external workspace.                 |
| `${CLOUDGENE_WORKSPACE_HOME}`| The location of the external workspace.         |


## Example

```yaml
id: print-username
name: Print Username Example
version: 1.0.0
workflow:
  steps:
    - name: "Say hello"
      type: "command"
	  cmd: "/bin/echo hey ${CLOUDGENE_USER_NAME}"
	  stdout: "true"
```
