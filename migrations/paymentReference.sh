#!/bin/bash

echo ""
echo "Applying migration paymentReference"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /paymentReference                        controllers.paymentReferenceController.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /paymentReference                        controllers.paymentReferenceController.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /changepaymentReference                  controllers.paymentReferenceController.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /changepaymentReference                  controllers.paymentReferenceController.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "paymentReference.title = paymentReference" >> ../conf/messages.en
echo "paymentReference.heading = paymentReference" >> ../conf/messages.en
echo "paymentReference.checkYourAnswersLabel = paymentReference" >> ../conf/messages.en
echo "paymentReference.error.required = Enter paymentReference" >> ../conf/messages.en
echo "paymentReference.error.length = paymentReference must be 100 characters or less" >> ../conf/messages.en
echo "paymentReference.change.hidden = paymentReference" >> ../conf/messages.en

echo "Migration paymentReference completed"
