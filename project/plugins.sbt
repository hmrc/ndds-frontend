resolvers += "HMRC-open-artefacts-maven" at "https://open.artefacts.tax.service.gov.uk/maven2"

resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(Resolver.ivyStylePatterns)

resolvers += "Typesafe Releases" at "https://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("uk.gov.hmrc"         % "sbt-auto-build"      % "3.24.0")
addSbtPlugin("uk.gov.hmrc"         % "sbt-distributables"  % "2.6.0")
addSbtPlugin("org.playframework"   % "sbt-plugin"          % "3.0.9")
addSbtPlugin("org.scoverage"       % "sbt-scoverage"       % "2.3.1")
// sbt-sass-compiler must be >0.11.0 due a bug with local test execution
// https://hmrcdigital.slack.com/archives/C518C88QN/p1742315985341079?thread_ts=1739892735.419799&cid=C518C88QN
addSbtPlugin("uk.gov.hmrc"         % "sbt-sass-compiler"   % "0.11.0")
addSbtPlugin("com.github.sbt"      % "sbt-concat"          % "1.0.0")
addSbtPlugin("com.github.sbt"      % "sbt-digest"          % "2.0.0")
addSbtPlugin("com.timushev.sbt"    % "sbt-updates"         % "0.6.4")
addSbtPlugin("org.scalameta"       % "sbt-scalafmt"        % "2.5.2")
