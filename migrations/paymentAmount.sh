#!/bin/bash

echo ""
echo "Applying migration paymentAmount"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /paymentAmount                  controllers.paymentAmountController.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /paymentAmount                  controllers.paymentAmountController.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /changepaymentAmount                        controllers.paymentAmountController.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /changepaymentAmount                        controllers.paymentAmountController.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "paymentAmount.title = paymentAmount" >> ../conf/messages.en
echo "paymentAmount.heading = paymentAmount" >> ../conf/messages.en
echo "paymentAmount.checkYourAnswersLabel = paymentAmount" >> ../conf/messages.en
echo "paymentAmount.error.nonNumeric = Enter your paymentAmount using numbers and a decimal point" >> ../conf/messages.en
echo "paymentAmount.error.required = Enter your paymentAmount" >> ../conf/messages.en
echo "paymentAmount.error.invalidNumeric = Enter your paymentAmount using up to two decimal places" >> ../conf/messages.en
echo "paymentAmount.error.aboveMaximum = paymentAmount must be {0} or less" >> ../conf/messages.en
echo "paymentAmount.error.belowMinimum = paymentAmount must be {0} or more" >> ../conf/messages.en
echo "paymentAmount.change.hidden = paymentAmount" >> ../conf/messages.en

echo "Migration paymentAmount completed"
