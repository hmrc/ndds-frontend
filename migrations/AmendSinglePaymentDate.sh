#!/bin/bash

echo ""
echo "Applying migration AmendSinglePaymentDate"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /amendSinglePaymentDate                  controllers.AmendSinglePaymentDateController.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /amendSinglePaymentDate                  controllers.AmendSinglePaymentDateController.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /changeAmendSinglePaymentDate                        controllers.AmendSinglePaymentDateController.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /changeAmendSinglePaymentDate                        controllers.AmendSinglePaymentDateController.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "amendSinglePaymentDate.title = AmendSinglePaymentDate" >> ../conf/messages.en
echo "amendSinglePaymentDate.heading = AmendSinglePaymentDate" >> ../conf/messages.en
echo "amendSinglePaymentDate.hint = For example, 12 11 2007." >> ../conf/messages.en
echo "amendSinglePaymentDate.checkYourAnswersLabel = AmendSinglePaymentDate" >> ../conf/messages.en
echo "amendSinglePaymentDate.error.required.all = Enter the amendSinglePaymentDate" >> ../conf/messages.en
echo "amendSinglePaymentDate.error.required.two = The amendSinglePaymentDate" must include {0} and {1} >> ../conf/messages.en
echo "amendSinglePaymentDate.error.required = The amendSinglePaymentDate must include {0}" >> ../conf/messages.en
echo "amendSinglePaymentDate.error.invalid = Enter a real AmendSinglePaymentDate" >> ../conf/messages.en
echo "amendSinglePaymentDate.change.hidden = AmendSinglePaymentDate" >> ../conf/messages.en

echo "Migration AmendSinglePaymentDate completed"
