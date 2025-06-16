# Welcome to CDK Java project!

This is a project for CDK development with ECS fargate service with mysqldb and ci/cd pipline implemented

The `cdk.json` file tells the CDK Toolkit how to execute your app.

## Useful commands

 * `set ACCOUNT_ID={account_id}`
 * `aws secretsmanager create-secret   --name GITHUB_TOKEN   --secret-string "<GIT_TOKEN>"`
 * `mvn package`     compile and run tests
 * `cdk bootstrap  --cloudformation-execution-policies arn:aws:iam::aws:policy/AdministratorAccess   aws://<account-id>/ap-south-1`
 * `cdk synth`       emits the synthesized CloudFormation template
 * `cdk deploy stack-*`      deploy this stack to your default region is ap-south-1
 

