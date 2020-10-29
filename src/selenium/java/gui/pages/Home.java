package gui.pages;

import org.fluentlenium.core.FluentPage;
import org.fluentlenium.core.annotation.PageUrl;
import org.fluentlenium.core.domain.FluentWebElement;
import org.openqa.selenium.support.FindBy;

import static org.fluentlenium.core.filter.FilterConstructor.*;


@PageUrl("http://localhost:3000")
public class Home extends FluentPage {

    @FindBy(id = "home")
    public FluentWebElement home;

    public QuizzPage selectQuizz(String quizz) {
        $(withId(quizz)).await().atMost(1000).until().displayed();
        $(withId(quizz)).click();
        return newInstance(QuizzPage.class);

    }

}
