
# ndds-frontend

The NDDS Replatform Portal is the frontend service used to create links to a user's bank account so that they then create payment plans for specific tax services to pay amounts due.

National Direct Debit System (NDDS), is a service that allows users to pay taxes via direct debits. Users can either opt for recurring payments or one of direct debit payments. The service currently supports setting up direct debits for multiple services.

The services supported are:

- VAT
- SA
- MGD
- NIC
- CT
- PAYE
- SDLT
- TC
- Other Liability

## Developer setup
[Developer setup](https://confluence.tools.tax.service.gov.uk/display/RBD/Local+Machine+Setup+to+run+and+connect+to+Oracle+database)

## Running the service locally

Service Manager for NDDS: `sm2 --start NDDS_ALL`

To check libraries update, run all tests and coverage: `./run_all_tests.sh`

To start the server locally: `sbt run` or `sbt run 6990`

To execute the scala formatter: `./run_all_checks.sh`

Reference to Testing data: https://confluence.tools.tax.service.gov.uk/x/67m5B


## Test-Only Functionality

To run in test-only mode make sure these values are set in the config:
```HOCON
play.http.router = testOnlyDoNotUseInAppConf.Routes
clock-module     = testOnly.config.Module
```
Locally, running the `run_test_only.sh` script will ensure this is set up correctly.

### /direct-debits/test-only/clock

Local link: http://localhost:6990/direct-debits/test-only/clock

On this page you can manipulate the clock used by the application. By default, the clock used will be system UTC.
The page allows you to make the clock fixed to a specific date and time or to reset to the default clock to be 
able to test time-specific behaviour. Be careful changing the clock as it will affect all users of the service, 
not just your session.



### License

This code is /open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").