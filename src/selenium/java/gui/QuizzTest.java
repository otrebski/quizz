package gui;

import gui.pages.Home;
import org.fluentlenium.adapter.junit.FluentTest;
import org.fluentlenium.core.annotation.Page;
import org.junit.Test;

//@FluentConfiguration(capabilities = "{\"chromeOptions\": {\"args\": [\"headless\",\"disable-gpu\"]}}")
public class QuizzTest extends FluentTest {

    @Page
    Home homePage;

    @Test
    public void goThroughQuizz() {
        //TODO upload quizz
        // * Question 1
        //  - Answer 1.1
        //   * Question 1.1.1
        //   * Question 1.1.2
        //  - Answer 1.2
        //   * Question 1.2.1
        //    - Answer 1.2.1 1
        //    - Answer 1.2.1 2
        //   * Question 1.2.2
        //
        
        goTo(homePage)
                .selectQuizz("simple_tree.json")
                .select("Right")
                .select("Right")
                .validateIsFinal("Right Right Node");
    }

    @Override
    public String getWebDriver() {
        return "chrome";
    }
}