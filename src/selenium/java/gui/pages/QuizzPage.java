package gui.pages;

import org.fluentlenium.core.FluentPage;
import org.fluentlenium.core.annotation.Page;
import org.fluentlenium.core.domain.FluentList;
import org.fluentlenium.core.domain.FluentWebElement;

import java.time.Duration;

import static org.fluentlenium.core.filter.FilterConstructor.*;

public class QuizzPage extends FluentPage {

    @Page
    Home home;

    private final Duration tenSeconds = Duration.ofSeconds(10);

    public QuizzPage select(String option) {
        FluentList<FluentWebElement> question = $(withId("question")).
                $(withText(option));
        question.await().atMost(tenSeconds).until().displayed();
        question.click();
        return this;
    }

    public QuizzPage selectHistory(String option) {
        $(withClass("historyStep")).$(withText(option)).click();
        return this;
    }

    public QuizzPage validateIsFinal(String solution){
        $(withId("final_step")).$(withText(solution)).await().atMost(tenSeconds).until().displayed();
        return this;
    }

    public Home goHome(){
        return goTo(home);
    }

}
