# Iceberg REST Lambda

To build:

```shell
./gradlew shadowJar
```

To deploy, you need SAM. If it is not available install it [here](https://docs.aws.amazon.com/serverless-application-model/latest/developerguide/install-sam-cli.html).
Then run:

```shell
sam package --s3-bucket <sam-deploy-artifact-bucket-name> --region <region-name>
sam deploy --guided # first time, or just `sam deploy` going forward
```
