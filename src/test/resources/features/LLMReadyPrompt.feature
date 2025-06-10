Feature: LLM Ready Prompt

  Scenario: Verify page elements using LLM
    Given I open the browser and navigate to "https://www.google.com"
    When I analyze the page elements using LLM
    Then I should validate the page content using LLM