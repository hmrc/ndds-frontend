
# ndds-frontend

The NDDS Replatform Portal is the frontend service used to create links to a user's bank account so that they then create payment plans for specific tax services to pay amounts due.

National Direct Debit System (NDDS), is a service that allows users to pay taxes via direct debits. Users can either opt for recurring payments or one of direct debit payments. The service currently supports setting up direct debits for multiple services.

The services supported are:

VAT
SA
MGD
NIC
CT
PAYE
SDLT
TC
Other Liability

## Developer setup
[Developer setup](https://confluence.tools.tax.service.gov.uk/display/RBD/Local+Machine+Setup+to+run+and+connect+to+Oracle+database)

## Running the service locally

Service Manager for NDDS: `sm2 --start NDDS_ALL`

To check libraries update, run all tests and coverage: `./run_all_tests.sh`

To start the server locally: `sbt run` or `sbt run 6990`

To execute the scala formatter: `./run_all_checks.sh`


### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html").