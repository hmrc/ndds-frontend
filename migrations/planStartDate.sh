#!/bin/bash

echo ""
echo "Applying migration planStartDate"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /planStartDate                  controllers.planStartDateController.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /planStartDate                  controllers.planStartDateController.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /changeplanStartDate                        controllers.planStartDateController.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /changeplanStartDate                        controllers.planStartDateController.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "planStartDate.title = planStartDate" >> ../conf/messages.en
echo "planStartDate.heading = planStartDate" >> ../conf/messages.en
echo "planStartDate.hint = For example, 12 11 2007." >> ../conf/messages.en
echo "planStartDate.checkYourAnswersLabel = planStartDate" >> ../conf/messages.en
echo "planStartDate.error.required.all = Enter the planStartDate" >> ../conf/messages.en
echo "planStartDate.error.required.two = The planStartDate" must include {0} and {1} >> ../conf/messages.en
echo "planStartDate.error.required = The planStartDate must include {0}" >> ../conf/messages.en
echo "planStartDate.error.invalid = Enter a real planStartDate" >> ../conf/messages.en
echo "planStartDate.change.hidden = planStartDate" >> ../conf/messages.en

echo "Migration planStartDate completed"
