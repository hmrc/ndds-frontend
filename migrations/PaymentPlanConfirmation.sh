#!/bin/bash

echo ""
echo "Applying migration PaymentPlanConfirmation"

echo "Adding routes to conf/app.routes"
echo "" >> ../conf/app.routes
echo "GET        /paymentPlanConfirmation                       controllers.PaymentPlanConfirmationController.onPageLoad()" >> ../conf/app.routes

echo "Adding messages to conf.messages"
echo "" >> ../conf/messages.en
echo "paymentPlanConfirmation.title = paymentPlanConfirmation" >> ../conf/messages.en
echo "paymentPlanConfirmation.heading = paymentPlanConfirmation" >> ../conf/messages.en

echo "Migration PaymentPlanConfirmation completed"
