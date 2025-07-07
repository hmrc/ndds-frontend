#!/bin/bash

echo ""
echo "Applying migration yearEndAndMonth"

echo "Adding routes to conf/app.routes"

echo "" >> ../conf/app.routes
echo "GET        /yearEndAndMonth                  controllers.yearEndAndMonthController.onPageLoad(mode: Mode = NormalMode)" >> ../conf/app.routes
echo "POST       /yearEndAndMonth                  controllers.yearEndAndMonthController.onSubmit(mode: Mode = NormalMode)" >> ../conf/app.routes

echo "GET        /changeyearEndAndMonth                        controllers.yearEndAndMonthController.onPageLoad(mode: Mode = CheckMode)" >> ../conf/app.routes
echo "POST       /changeyearEndAndMonth                        controllers.yearEndAndMonthController.onSubmit(mode: Mode = CheckMode)" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "yearEndAndMonth.title = yearEndAndMonth" >> ../conf/messages.en
echo "yearEndAndMonth.heading = yearEndAndMonth" >> ../conf/messages.en
echo "yearEndAndMonth.hint = For example, 12 11 2007." >> ../conf/messages.en
echo "yearEndAndMonth.checkYourAnswersLabel = yearEndAndMonth" >> ../conf/messages.en
echo "yearEndAndMonth.error.required.all = Enter the yearEndAndMonth" >> ../conf/messages.en
echo "yearEndAndMonth.error.required.two = The yearEndAndMonth" must include {0} and {1} >> ../conf/messages.en
echo "yearEndAndMonth.error.required = The yearEndAndMonth must include {0}" >> ../conf/messages.en
echo "yearEndAndMonth.error.invalid = Enter a real yearEndAndMonth" >> ../conf/messages.en
echo "yearEndAndMonth.change.hidden = yearEndAndMonth" >> ../conf/messages.en

echo "Migration yearEndAndMonth completed"
