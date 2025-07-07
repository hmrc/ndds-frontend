#!/bin/bash

echo ""
echo "Applying migration paymentDate"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /paymentDate                  controllers.paymentDateController.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /paymentDate                  controllers.paymentDateController.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /changepaymentDate                        controllers.paymentDateController.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /changepaymentDate                        controllers.paymentDateController.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "paymentDate.title = paymentDate" >> ../conf/messages.en
echo "paymentDate.heading = paymentDate" >> ../conf/messages.en
echo "paymentDate.hint = For example, 12 11 2007." >> ../conf/messages.en
echo "paymentDate.checkYourAnswersLabel = paymentDate" >> ../conf/messages.en
echo "paymentDate.error.required.all = Enter the paymentDate" >> ../conf/messages.en
echo "paymentDate.error.required.two = The paymentDate" must include {0} and {1} >> ../conf/messages.en
echo "paymentDate.error.required = The paymentDate must include {0}" >> ../conf/messages.en
echo "paymentDate.error.invalid = Enter a real paymentDate" >> ../conf/messages.en
echo "paymentDate.change.hidden = paymentDate" >> ../conf/messages.en

echo "Migration paymentDate completed"
