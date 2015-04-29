
Feature: Daily updates of available CRAN and BioConductor packages

  Scenario: New CRAN package available
    When a new package version is released on CRAN
    Then the new version should be listed on the front page
    And it should be built with the latest version of Renjin
    Given that the build succeeds
    Then the JAR should be available in the Renjin repository
    And tests should be visible on the package version page 
    
    