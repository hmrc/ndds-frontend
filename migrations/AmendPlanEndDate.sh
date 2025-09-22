#!/bin/bash

echo ""
echo "Applying migration AmendPlanEndDate"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /amendPlanEndDate                  controllers.AmendPlanEndDateController.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /amendPlanEndDate                  controllers.AmendPlanEndDateController.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /changeAmendPlanEndDate                        controllers.AmendPlanEndDateController.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /changeAmendPlanEndDate                        controllers.AmendPlanEndDateController.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "amendPlanEndDate.title = AmendPlanEndDate" >> ../conf/messages.en
echo "amendPlanEndDate.heading = AmendPlanEndDate" >> ../conf/messages.en
echo "amendPlanEndDate.hint = For example, 12 11 2007." >> ../conf/messages.en
echo "amendPlanEndDate.checkYourAnswersLabel = AmendPlanEndDate" >> ../conf/messages.en
echo "amendPlanEndDate.error.required.all = Enter the amendPlanEndDate" >> ../conf/messages.en
echo "amendPlanEndDate.error.required.two = The amendPlanEndDate" must include {0} and {1} >> ../conf/messages.en
echo "amendPlanEndDate.error.required = The amendPlanEndDate must include {0}" >> ../conf/messages.en
echo "amendPlanEndDate.error.invalid = Enter a real AmendPlanEndDate" >> ../conf/messages.en
echo "amendPlanEndDate.change.hidden = AmendPlanEndDate" >> ../conf/messages.en

echo "Migration AmendPlanEndDate completed"
